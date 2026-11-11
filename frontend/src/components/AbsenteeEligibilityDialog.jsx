import { useEffect, useState } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import api from '../services/api';

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(`${dateStr}T00:00:00`).toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const [hours, minutes] = timeStr.split(':');
  const hour = parseInt(hours, 10);
  return `${hour % 12 || 12}:${minutes} ${hour >= 12 ? 'PM' : 'AM'}`;
}

function todayIsoDate() {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  return new Date(now.getTime() - offset * 60 * 1000).toISOString().slice(0, 10);
}

export default function AbsenteeEligibilityDialog({
  open,
  student,
  enrollmentNumber,
  selfService = false,
  onClose,
  onSaved,
}) {
  const [exams, setExams] = useState([]);
  const [selected, setSelected] = useState(new Set());
  const [locked, setLocked] = useState(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [verification, setVerification] = useState(null);
  const [error, setError] = useState('');

  const endpoint = selfService
    ? `/api/students/${encodeURIComponent(enrollmentNumber || '')}/absentee-eligibility`
    : student
      ? `/api/students/${student.studentId}/absentee-exams`
      : null;

  useEffect(() => {
    if (!open) return;
    setExams([]);
    setSelected(new Set());
    setLocked(new Set());
    setVerification(null);
    setDateOfBirth('');
    setError('');
  }, [open, endpoint]);

  useEffect(() => {
    if (!open || !endpoint) return undefined;
    if (selfService && (!verification || verification.enrollment !== enrollmentNumber)) {
      return undefined;
    }

    let cancelled = false;
    const loadOptions = async () => {
      setLoading(true);
      setError('');
      try {
        const headers = selfService
          ? { 'X-Student-Verification': verification.token }
          : undefined;
        const response = await api.get(endpoint, { headers });
        if (cancelled) return;
        setExams(response.data);
        setSelected(new Set(
          response.data.filter((exam) => exam.assigned).map((exam) => exam.examId)
        ));
        setLocked(new Set(
          response.data.filter((exam) => exam.assigned && exam.locked)
            .map((exam) => exam.examId)
        ));
      } catch (err) {
        if (cancelled) return;
        if (selfService && err.response?.status === 401) {
          setVerification(null);
        }
        setError(err.response?.data?.message || 'Failed to load absentee papers.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadOptions();
    return () => { cancelled = true; };
  }, [open, endpoint, selfService, verification, enrollmentNumber]);

  const handleVerify = async (event) => {
    event.preventDefault();
    if (!dateOfBirth) {
      setError('Enter your date of birth to continue.');
      return;
    }
    setVerifying(true);
    setError('');
    try {
      const response = await api.post(
        `/api/students/${encodeURIComponent(enrollmentNumber || '')}/verify`,
        { dateOfBirth }
      );
      setVerification({
        token: response.data.token,
        enrollment: enrollmentNumber,
      });
    } catch (err) {
      setError(err.response?.data?.message
        || 'Enrollment number or date of birth is incorrect.');
    } finally {
      setVerifying(false);
    }
  };

  const toggleExam = (examId) => {
    const exam = exams.find((item) => item.examId === examId);
    if (selfService && exam?.locked) return;

    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(examId)) {
        next.delete(examId);
      } else {
        next.add(examId);
      }
      return next;
    });

    if (!selfService) {
      setLocked((previous) => {
        const next = new Set(previous);
        if (selected.has(examId)) {
          next.delete(examId);
        } else {
          // New admin selections are protected by default.
          next.add(examId);
        }
        return next;
      });
    }
  };

  const toggleLock = (examId) => {
    setLocked((previous) => {
      const next = new Set(previous);
      if (next.has(examId)) {
        next.delete(examId);
      } else {
        next.add(examId);
      }
      return next;
    });
  };

  const selectAll = () => {
    setSelected(new Set(exams.map((exam) => exam.examId)));
    if (!selfService) {
      setLocked(new Set(exams.map((exam) => exam.examId)));
    }
  };

  const clearSelection = () => {
    if (selfService) {
      setSelected(new Set(locked));
      return;
    }
    setSelected(new Set());
    setLocked(new Set());
  };

  const handleSave = async () => {
    if (!endpoint || (selfService && !verification)) return;
    setSaving(true);
    setError('');
    try {
      const payload = { examIds: [...selected] };
      if (!selfService) {
        payload.lockedExamIds = [...locked].filter((examId) => selected.has(examId));
      }
      await api.put(endpoint, payload, selfService ? {
        headers: { 'X-Student-Verification': verification.token },
      } : undefined);
      onSaved?.();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save absentee papers.');
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => {
    if (!saving && !verifying) onClose();
  };

  const showOptions = !selfService || Boolean(verification);

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth aria-labelledby="absentee-dialog-title">
      <DialogTitle id="absentee-dialog-title">
        {selfService ? 'I Was Absent' : 'Manage Absentee Papers'}
      </DialogTitle>
      <DialogContent>
        <Typography sx={{ color: 'text.secondary', mb: 2 }}>
          {selfService
            ? `Enrollment: ${enrollmentNumber || ''}`
            : student
              ? `${student.name} · ${student.enrollmentNumber}`
              : ''}
        </Typography>

        {selfService && !showOptions ? (
          <Box component="form" onSubmit={handleVerify}>
            <Alert severity="info" sx={{ mb: 2 }}>
              Verify your identity with your enrollment number and date of birth.
              The verification is valid for a few minutes.
            </Alert>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <TextField
              label="Date of birth"
              type="date"
              value={dateOfBirth}
              onChange={(event) => {
                setDateOfBirth(event.target.value);
                setError('');
              }}
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { max: todayIsoDate(), 'aria-label': 'Date of birth' },
              }}
              fullWidth
              autoFocus
              sx={{ mb: 2 }}
            />
            <Button
              type="submit"
              variant="contained"
              disabled={verifying}
              sx={{
                background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' },
              }}
            >
              {verifying ? 'Verifying...' : 'Verify and continue'}
            </Button>
          </Box>
        ) : (
          <>
            <Alert severity="info" sx={{ mb: 2 }}>
              {selfService
                ? 'Check each paper you missed. Papers locked by an administrator cannot be removed. Your schedule will show the absentee paper instead of the regular paper.'
                : 'Checked papers replace the regular paper for that subject in the student’s schedule. Locked papers cannot be removed by the student.'}
            </Alert>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
                <CircularProgress size={32} />
              </Box>
            ) : exams.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 6 }}>
                <EventBusyIcon sx={{ fontSize: 48, color: '#B0B0B0', opacity: 0.35, mb: 1 }} />
                <Typography sx={{ color: 'text.secondary' }}>
                  {selfService
                    ? 'No absentee papers are available for your department and semester.'
                    : 'No absentee papers are available for this student’s department and semester.'}
                </Typography>
              </Box>
            ) : (
              <>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
                    {exams.length} absentee paper{exams.length === 1 ? '' : 's'} available
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button size="small" onClick={selectAll}>
                      Select all
                    </Button>
                    <Button size="small" onClick={clearSelection}>
                      Clear
                    </Button>
                  </Box>
                </Box>
                <Divider />
                <Box>
                  {exams.map((exam) => (
                    <Box
                      key={exam.examId}
                      sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 1,
                        py: 1.5,
                        borderBottom: '1px solid rgba(255,255,255,0.06)',
                      }}
                    >
                      <Checkbox
                        checked={selected.has(exam.examId)}
                        onChange={() => toggleExam(exam.examId)}
                        disabled={selfService && exam.locked}
                        slotProps={{
                          input: {
                            'aria-label': selfService
                              ? `Mark ${exam.subjectName} absentee paper as missed`
                              : `Assign ${exam.subjectName} absentee paper`,
                          },
                        }}
                        sx={{ color: '#FF8F00', '&.Mui-checked': { color: '#FF8F00' } }}
                      />
                      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography sx={{ fontWeight: 600 }}>{exam.subjectName}</Typography>
                        <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap', mt: 0.5 }}>
                          <Chip label={exam.subjectCode} size="small" variant="outlined"
                            sx={{ borderColor: '#FF8F00', color: '#FFB300', fontWeight: 600 }} />
                          <Chip label="ABSENTEE" size="small"
                            sx={{
                              fontWeight: 700,
                              fontSize: '0.68rem',
                              backgroundColor: 'rgba(230, 81, 0, 0.18)',
                              color: '#FFB300',
                              border: '1px solid #FF8F00',
                            }} />
                          {exam.assigned && exam.locked && (
                            <Chip
                              icon={<LockIcon />}
                              label="Locked by admin"
                              size="small"
                              sx={{
                                fontWeight: 600,
                                backgroundColor: 'rgba(255, 183, 77, 0.12)',
                                color: '#FFB74D',
                                border: '1px solid rgba(255, 183, 77, 0.4)',
                              }}
                            />
                          )}
                        </Stack>
                        <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.75 }}>
                          {formatDate(exam.examDate)} · {formatTime(exam.examTime)}
                          {exam.examEndTime ? ` – ${formatTime(exam.examEndTime)}` : ''}
                        </Typography>
                      </Box>
                      {!selfService && (
                        <Tooltip title={locked.has(exam.examId)
                          ? 'Locked: students cannot remove this paper'
                          : 'Unlocked: students may remove this paper'}>
                          <span>
                            <IconButton
                              size="small"
                              onClick={() => toggleLock(exam.examId)}
                              disabled={!selected.has(exam.examId)}
                              aria-label={locked.has(exam.examId)
                                ? `Unlock ${exam.subjectName} absentee paper`
                                : `Lock ${exam.subjectName} absentee paper`}
                              sx={{ color: locked.has(exam.examId) ? '#FFB74D' : '#B0B0B0' }}
                            >
                              {locked.has(exam.examId) ? <LockIcon fontSize="small" /> : <LockOpenIcon fontSize="small" />}
                            </IconButton>
                          </span>
                        </Tooltip>
                      )}
                    </Box>
                  ))}
                </Box>
              </>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose} color="inherit" variant="outlined" disabled={saving || verifying}>
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={!showOptions || loading || saving || verifying}
          sx={{
            background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
            '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' },
          }}
        >
          {saving ? 'Saving...' : selfService ? 'Save my absence' : 'Save eligibility'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
