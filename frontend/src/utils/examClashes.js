import { getExamKey } from './personalSchedule.js';

export function parseExamTime(value) {
  if (typeof value !== 'string') return null;
  const match = value.trim().match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

function overlaps(first, second) {
  const firstStart = parseExamTime(first.examTime);
  const secondStart = parseExamTime(second.examTime);
  if (firstStart === null || secondStart === null) return false;

  const firstEnd = parseExamTime(first.examEndTime) ?? firstStart;
  const secondEnd = parseExamTime(second.examEndTime) ?? secondStart;

  if (firstStart === secondStart) return true;
  return firstStart < secondEnd && secondStart < firstEnd;
}

/**
 * Finds time clashes among saved exams. Exams are compared only within the
 * same enrollment number, so two different students' papers never conflict.
 * Papers without end times are treated as a single point in time; an exact
 * start-time match still clashes, but overlapping unknown durations do not.
 */
export function findExamClashes(exams) {
  const byEnrollment = new Map();

  exams.forEach((exam) => {
    const enrollment = String(exam.enrollmentNumber || '').trim().toUpperCase();
    if (!byEnrollment.has(enrollment)) byEnrollment.set(enrollment, []);
    byEnrollment.get(enrollment).push(exam);
  });

  const clashes = [];
  byEnrollment.forEach((studentExams, enrollment) => {
    for (let firstIndex = 0; firstIndex < studentExams.length; firstIndex += 1) {
      for (let secondIndex = firstIndex + 1; secondIndex < studentExams.length; secondIndex += 1) {
        const first = studentExams[firstIndex];
        const second = studentExams[secondIndex];
        if (!first.examDate || first.examDate !== second.examDate) continue;
        if (!overlaps(first, second)) continue;

        clashes.push({
          enrollment,
          first,
          second,
          firstKey: getExamKey(first, first.enrollmentNumber),
          secondKey: getExamKey(second, second.enrollmentNumber),
        });
      }
    }
  });

  return clashes.sort((left, right) => {
    const dateOrder = left.first.examDate.localeCompare(right.first.examDate);
    if (dateOrder !== 0) return dateOrder;
    return (parseExamTime(left.first.examTime) ?? 0) - (parseExamTime(right.first.examTime) ?? 0);
  });
}
