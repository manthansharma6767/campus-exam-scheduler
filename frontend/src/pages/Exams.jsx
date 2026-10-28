import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import IconButton from '@mui/material/IconButton';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import Fade from '@mui/material/Fade';
import Chip from '@mui/material/Chip';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import QuizIcon from '@mui/icons-material/Quiz';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
import ImportPreviewPanel from '../components/ImportPreviewPanel';
import api from '../services/api';

const emptyForm = { examDate: '', examTime: '', examEndTime: '', roomNumber: '', subjectId: '', building: '' };

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr + 'T00:00:00');
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const [hours, minutes] = timeStr.split(':');
  const h = parseInt(hours, 10);
  const ampm = h >= 12 ? 'PM' : 'AM';
  const h12 = h % 12 || 12;
  return `${h12}:${minutes} ${ampm}`;
}

export default function Exams() {
  const [exams, setExams] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [submitting, setSubmitting] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importPreview, setImportPreview] = useState(null);
  const [importError, setImportError] = useState('');
  const [importAction, setImportAction] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const [examRes, subRes] = await Promise.all([
        api.get('/api/exams'),
        api.get('/api/subjects'),
      ]);
      setExams(examRes.data);
      setSubjects(subRes.data);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load data', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setFormError('');
  };

  const openAdd = () => {
    setEditing(null);
    setForm(emptyForm);
    setFormError('');
    setDialogOpen(true);
  };

  const openEdit = (exam) => {
    setEditing(exam);
    setForm({
      examDate: exam.examDate || '',
      examTime: exam.examTime ? exam.examTime.substring(0, 5) : '',
      examEndTime: exam.examEndTime ? exam.examEndTime.substring(0, 5) : '',
      roomNumber: exam.roomNumber,
      subjectId: exam.subjectId,
      building: exam.building,
    });
    setFormError('');
    setDialogOpen(true);
  };

  const validate = () => {
    if (!form.examDate) return 'Exam date is required';
    if (!form.examTime) return 'Exam time is required';
    if (form.examEndTime && form.examEndTime <= form.examTime) return 'Exam end time must be later than its start time';
    if (!form.roomNumber.trim()) return 'Room number is required';
    if (!form.subjectId) return 'Subject is required';
    if (!form.building.trim()) return 'Building is required';
    return null;
  };

  const handleSubmit = async () => {
    const err = validate();
    if (err) { setFormError(err); return; }
    setSubmitting(true);
    try {
      const payload = {
        examDate: form.examDate,
        examTime: form.examTime + ':00',
        examEndTime: form.examEndTime ? `${form.examEndTime}:00` : null,
        roomNumber: form.roomNumber.trim(),
        subjectId: parseInt(form.subjectId, 10),
        building: form.building.trim(),
      };
      if (editing) {
        await api.put(`/api/exams/${editing.examId}`, payload);
        setSnackbar({ open: true, message: 'Exam updated successfully', severity: 'success' });
      } else {
        await api.post('/api/exams', payload);
        setSnackbar({ open: true, message: 'Exam created successfully', severity: 'success' });
      }
      setDialogOpen(false);
      fetchData();
    } catch (err) {
      setFormError(err.response?.data?.message || 'An error occurred');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    try {
      await api.delete(`/api/exams/${deleteTarget.examId}`);
      setSnackbar({ open: true, message: 'Exam deleted successfully', severity: 'success' });
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to delete', severity: 'error' });
      setConfirmOpen(false);
    }
  };

  const handleTimetablePreview = async () => {
    if (!importFile) return;
    setImporting(true);
    setImportAction('preview');
    setImportError('');
    try {
      const body = new FormData();
      body.append('file', importFile);
      const response = await api.post('/api/exams/import/preview', body);
      setImportPreview(response.data);
    } catch (err) {
      if (err.response?.data?.errors) setImportPreview(err.response.data);
      setImportError(err.response?.data?.message || 'Timetable preview failed. Check the file and try again.');
    } finally {
      setImporting(false);
      setImportAction('');
    }
  };

  const handleTimetableImport = async () => {
    if (!importFile || !importPreview?.canImport) return;
    setImporting(true);
    setImportAction('commit');
    setImportError('');
    try {
      const body = new FormData();
      body.append('file', importFile);
      const response = await api.post('/api/exams/import', body);
      setSnackbar({ open: true, message: response.data.message, severity: 'success' });
      setImportDialogOpen(false);
      setImportFile(null);
      setImportPreview(null);
      fetchData();
    } catch (err) {
      if (err.response?.data?.errors || err.response?.data?.conflicts) setImportPreview(err.response.data);
      setImportError(err.response?.data?.message || 'Timetable import failed. Check the file and try again.');
    } finally {
      setImporting(false);
      setImportAction('');
    }
  };

  const closeImportDialog = () => {
    if (importing) return;
    setImportDialogOpen(false);
    setImportFile(null);
    setImportPreview(null);
    setImportError('');
  };

  const handleTimetableFileSelection = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setImportFile(file);
    setImportPreview(null);
    setImportError('');
    setImportDialogOpen(true);
  };

  return (
    <Fade in timeout={600}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)', color: 'white', py: { xs: 3, md: 4 } }}>
          <Container maxWidth="lg">
            <Button component={Link} to="/admin/dashboard" startIcon={<ArrowBackIcon />}
              sx={{ color: 'rgba(255,255,255,0.7)', mb: 1, '&:hover': { color: '#FF8F00', backgroundColor: 'rgba(230, 81, 0, 0.08)' } }}>
              Back to Dashboard
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <QuizIcon sx={{ fontSize: 30, color: '#FF8F00' }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Exam Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '60vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1.5, mb: 3, flexWrap: 'wrap' }}>
              <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} disabled={importing}
                sx={{ borderColor: '#FF8F00', color: '#FFB300', '&:hover': { borderColor: '#FFB300' } }}>
                {importing ? 'Importing...' : 'Import timetable'}
                <input hidden type="file" accept=".pdf,.xls,.xlsx,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={handleTimetableFileSelection} />
              </Button>
              <Button id="add-exam-button" variant="contained" startIcon={<AddIcon />} onClick={openAdd}
                sx={{ background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)', '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' } }}>
                Add Exam
              </Button>
            </Box>

            <Typography variant="body2" sx={{ color: '#B0B0B0', textAlign: 'right', mb: 2 }}>
              Excel headers: Subject Code, Date, Time, Building, Room. Text-based PDFs support aligned rows and date-by-course matrices; missing venues import as “Not assigned”. Scanned/image-only PDFs need OCR first.
            </Typography>

            {loading ? (
              <Loading message="Loading exams..." />
            ) : exams.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <QuizIcon sx={{ fontSize: 64, color: '#B0B0B0', opacity: 0.2, mb: 2 }} />
                <Typography variant="h6" sx={{ color: '#B0B0B0' }}>No exams found</Typography>
              </Box>
            ) : (
              <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid rgba(255,255,255,0.06)', borderRadius: 3, overflowX: 'auto', backgroundColor: '#242424' }}>
                <Table aria-label="Exams table" sx={{ minWidth: 750 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Subject</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Time</TableCell>
                      <TableCell>Building</TableCell>
                      <TableCell>Room</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {exams.map((exam) => (
                      <TableRow key={exam.examId}>
                        <TableCell>
                          <Box>
                            <Typography variant="body2" sx={{ fontWeight: 600 }}>{exam.subjectName}</Typography>
                            <Chip label={exam.subjectCode} size="small" variant="outlined" sx={{ mt: 0.5, borderColor: '#FF8F00', color: '#FFB300' }} />
                          </Box>
                        </TableCell>
                        <TableCell>{formatDate(exam.examDate)}</TableCell>
                        <TableCell>{formatTime(exam.examTime)}{exam.examEndTime ? ` – ${formatTime(exam.examEndTime)}` : ''}</TableCell>
                        <TableCell>{exam.building}</TableCell>
                        <TableCell>{exam.roomNumber}</TableCell>
                        <TableCell align="right">
                          <IconButton onClick={() => openEdit(exam)} aria-label={`Edit exam ${exam.subjectName}`} size="small" sx={{ color: '#FF8F00' }}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton color="error" onClick={() => { setDeleteTarget(exam); setConfirmOpen(true); }} aria-label={`Delete exam ${exam.subjectName}`} size="small" sx={{ ml: 0.5 }}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Container>
        </Box>

        <Dialog open={importDialogOpen} onClose={closeImportDialog} maxWidth="md" fullWidth aria-labelledby="timetable-import-title">
          <DialogTitle id="timetable-import-title">Import timetable</DialogTitle>
          <DialogContent>
            {importError && <Alert severity="error" sx={{ mt: 1, mb: 2 }}>{importError}</Alert>}
            <Typography color="text.secondary" sx={{ mb: 2 }}>
              Review the parsed timetable and resolve validation errors or room/student schedule conflicts before saving. Nothing is saved during preview.
            </Typography>
            <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} disabled={importing}>
              Choose PDF or Excel file
              <input hidden type="file" accept=".pdf,.xls,.xlsx,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(event) => {
                  const file = event.target.files?.[0] || null;
                  event.target.value = '';
                  setImportFile(file);
                  setImportPreview(null);
                  setImportError('');
                }} />
            </Button>
            <Typography component="span" sx={{ ml: 1.5, color: 'text.secondary', overflowWrap: 'anywhere' }}>
              {importFile?.name || 'No file selected'}
            </Typography>
            <ImportPreviewPanel preview={importPreview} />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={closeImportDialog} color="inherit" variant="outlined" disabled={importing}>Cancel</Button>
            <Button onClick={handleTimetablePreview} variant="outlined" disabled={importing || !importFile}>
              {importing && importAction === 'preview' ? 'Checking...' : 'Preview timetable'}
            </Button>
            <Button onClick={handleTimetableImport} variant="contained" disabled={importing || !importFile || !importPreview?.canImport}>
              {importing && importAction === 'commit' ? 'Importing...' : 'Confirm import'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Add/Edit Dialog */}
        <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth aria-labelledby="exam-dialog-title">
          <DialogTitle id="exam-dialog-title">{editing ? 'Edit Exam' : 'Add Exam'}</DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2, mt: 1, borderRadius: 2, backgroundColor: 'rgba(239, 83, 80, 0.1)', color: '#EF5350' }}>{formError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <TextField name="subjectId" label="Subject" select value={form.subjectId} onChange={handleChange}
                slotProps={{ htmlInput: { 'aria-label': 'Subject' } }} fullWidth>
                {subjects.map((s) => (
                  <MenuItem key={s.subjectId} value={s.subjectId}>
                    {s.subjectCode} — {s.subjectName}
                  </MenuItem>
                ))}
              </TextField>
              <TextField name="examDate" label="Exam Date" type="date" value={form.examDate} onChange={handleChange}
                slotProps={{ inputLabel: { shrink: true }, htmlInput: { 'aria-label': 'Exam date' } }} fullWidth />
              <TextField name="examTime" label="Exam Time" type="time" value={form.examTime} onChange={handleChange}
                slotProps={{ inputLabel: { shrink: true }, htmlInput: { 'aria-label': 'Exam time' } }} fullWidth />
              <TextField name="examEndTime" label="Exam End Time (optional)" type="time" value={form.examEndTime} onChange={handleChange}
                helperText="Add an end time to detect exams that overlap even when they start at different times."
                slotProps={{ inputLabel: { shrink: true }, htmlInput: { 'aria-label': 'Exam end time' } }} fullWidth />
              <TextField name="building" label="Building" value={form.building} onChange={handleChange}
                placeholder="e.g. Main Building" slotProps={{ htmlInput: { 'aria-label': 'Building name', maxLength: 100 } }} fullWidth />
              <TextField name="roomNumber" label="Room Number" value={form.roomNumber} onChange={handleChange}
                placeholder="e.g. A-205" slotProps={{ htmlInput: { 'aria-label': 'Room number', maxLength: 20 } }} fullWidth />
            </Box>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setDialogOpen(false)} color="inherit" variant="outlined">Cancel</Button>
            <Button onClick={handleSubmit} variant="contained" disabled={submitting}
              sx={{ background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)', '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' } }}>
              {submitting ? 'Saving...' : editing ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </Dialog>

        <ConfirmDialog open={confirmOpen} title="Delete Exam"
          message={`Are you sure you want to delete the exam for "${deleteTarget?.subjectName}"? This action cannot be undone.`}
          onConfirm={handleDelete} onCancel={() => { setConfirmOpen(false); setDeleteTarget(null); }} />

        <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity={snackbar.severity} variant="filled" sx={{ borderRadius: 2 }}>{snackbar.message}</Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
