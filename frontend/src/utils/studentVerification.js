const STORAGE_KEY = 'student_verification';

function normalizeEnrollment(value) {
  return String(value || '').trim().toUpperCase();
}

export function readStudentVerification(enrollmentNumber) {
  const requested = normalizeEnrollment(enrollmentNumber);
  if (!requested) return null;
  try {
    const cached = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null');
    if (!cached?.token || normalizeEnrollment(cached.enrollment) !== requested) return null;
    if (!cached.expiresAt || cached.expiresAt <= Date.now()) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return { token: cached.token, enrollment: cached.enrollment };
  } catch {
    return null;
  }
}

export function saveStudentVerification(enrollmentNumber, token, expiresInSeconds) {
  const seconds = Number(expiresInSeconds);
  if (!enrollmentNumber || !token || !Number.isFinite(seconds) || seconds <= 0) return;
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      token,
      enrollment: enrollmentNumber,
      expiresAt: Date.now() + seconds * 1000,
    }));
  } catch {
    // A full or unavailable session storage should not block verification.
  }
}

export function clearStudentVerification() {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore storage failures.
  }
}
