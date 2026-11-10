const STORAGE_KEY = 'personal_exam_schedule';

export function getExamKey(exam, enrollmentNumber) {
  return [enrollmentNumber, exam.subjectCode, exam.examDate, exam.examTime, exam.examType || 'REGULAR']
    .map((part) => String(part || '').trim().toUpperCase())
    .join('|');
}

export function readPersonalSchedule() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

export function saveExamToPersonalSchedule(exam, enrollmentNumber) {
  const saved = readPersonalSchedule();
  const key = getExamKey(exam, enrollmentNumber);
  if (saved.some((item) => getExamKey(item, item.enrollmentNumber) === key)) return saved;
  const updated = [...saved, { ...exam, enrollmentNumber }];
  localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  return updated;
}

export function removeExamFromPersonalSchedule(exam, enrollmentNumber) {
  const key = getExamKey(exam, enrollmentNumber);
  const updated = readPersonalSchedule().filter(
    (item) => getExamKey(item, item.enrollmentNumber) !== key
  );
  localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  return updated;
}

/**
 * Keep saved calendar entries in sync with the student's current schedule.
 * When a subject switches between regular and absentee, the saved entry is
 * replaced with the paper that is now scheduled for that subject.
 */
export function reconcilePersonalSchedule(exams, enrollmentNumber) {
  const currentEnrollment = String(enrollmentNumber || '').trim().toUpperCase();
  const scheduleByKey = new Map();
  const scheduleBySubject = new Map();

  exams.forEach((exam) => {
    scheduleByKey.set(getExamKey(exam, enrollmentNumber), exam);
    const subjectKey = String(exam.subjectCode || '').trim().toUpperCase();
    if (subjectKey && !scheduleBySubject.has(subjectKey)) {
      scheduleBySubject.set(subjectKey, exam);
    }
  });

  const allSaved = readPersonalSchedule();
  const otherEnrollments = allSaved.filter(
    (item) => String(item.enrollmentNumber || '').trim().toUpperCase() !== currentEnrollment
  );
  const currentEnrollmentSaved = [];
  const seenKeys = new Set();
  let changed = false;

  allSaved.forEach((item) => {
    if (String(item.enrollmentNumber || '').trim().toUpperCase() !== currentEnrollment) return;

    const savedKey = getExamKey(item, item.enrollmentNumber);
    const replacement = scheduleByKey.get(savedKey)
      || scheduleBySubject.get(String(item.subjectCode || '').trim().toUpperCase());

    if (!replacement) {
      changed = true;
      return;
    }

    const replacementEntry = { ...replacement, enrollmentNumber };
    const replacementKey = getExamKey(replacementEntry, enrollmentNumber);
    if (seenKeys.has(replacementKey)) {
      changed = true;
      return;
    }

    seenKeys.add(replacementKey);
    currentEnrollmentSaved.push(replacementEntry);
    if (savedKey !== replacementKey) changed = true;
  });

  if (changed) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...otherEnrollments, ...currentEnrollmentSaved]));
  }

  return currentEnrollmentSaved;
}

function escapeIcs(value) {
  return String(value || '')
    .replaceAll('\\', '\\\\')
    .replace(/\r\n|\r|\n/g, '\\n')
    .replaceAll(',', '\\,')
    .replaceAll(';', '\\;');
}

function foldIcsLine(line) {
  const chunks = [];
  let chunk = '';
  let bytes = 0;
  for (const character of line) {
    const size = new TextEncoder().encode(character).length;
    if (bytes + size > 73) {
      chunks.push(chunk);
      chunk = ` ${character}`;
      bytes = size + 1;
    } else {
      chunk += character;
      bytes += size;
    }
  }
  chunks.push(chunk);
  return chunks.join('\r\n');
}

function eventStartDate(date, time) {
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  // The campus schedule uses India Standard Time (UTC+05:30); emit UTC for broad calendar support.
  const utc = new Date(Date.UTC(year, month - 1, day, hour, minute - 330));
  return utc;
}

export function downloadCalendar(exams, durationMinutes = 180) {
  const events = exams.flatMap((exam) => {
    if (!exam.examDate || !exam.examTime) return [];
    const start = eventStartDate(exam.examDate, exam.examTime);
    const statedEnd = exam.examEndTime ? eventStartDate(exam.examDate, exam.examEndTime) : null;
    const end = statedEnd && statedEnd > start
      ? statedEnd
      : new Date(start.getTime() + durationMinutes * 60_000);
    const format = (date) => date.toISOString().replaceAll('-', '').replaceAll(':', '').replace(/\.\d{3}Z$/, 'Z');
    const uid = `${getExamKey(exam, exam.enrollmentNumber).replace(/[^A-Z0-9|]/g, '-')}.campusexamscheduler`;
    return [
      'BEGIN:VEVENT',
      `UID:${uid}`,
      `DTSTAMP:${format(new Date())}`,
      `DTSTART:${format(start)}`,
      `DTEND:${format(end)}`,
      `SUMMARY:${escapeIcs(`${exam.subjectCode} - ${exam.subjectName}`)}`,
      `LOCATION:${escapeIcs(`${exam.building}, Room ${exam.roomNumber}`)}`,
      `DESCRIPTION:${escapeIcs(`${exam.subjectCode} ${exam.examType === 'ABSENTEE' ? 'absentee ' : ''}exam`)}`,
      'END:VEVENT',
    ];
  });
  if (events.length === 0) return false;
  const lines = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Campus Exam Scheduler//Exam Schedule//EN', 'CALSCALE:GREGORIAN', ...events, 'END:VCALENDAR'];
  const blob = new Blob([lines.map(foldIcsLine).join('\r\n')], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'my-exam-schedule.ics';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  return true;
}
