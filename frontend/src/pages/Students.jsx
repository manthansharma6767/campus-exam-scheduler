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
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PeopleIcon from '@mui/icons-material/People';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
import ImportPreviewPanel from '../components/ImportPreviewPanel';
import api from '../services/api';

const emptyForm = { name: '', email: '', password: '', enrollmentNumber: '', semester: '', departmentId: '' };

export default function Students() {
  const [students, setStudents] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [submitting, setSubmitting] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importError, setImportError] = useState('');
  const [importing, setImporting] = useState(false);
  const [importAction, setImportAction] = useState('');
  const [importPreview, setImportPreview] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [stuRes, deptRes] = await Promise.all([
        api.get('/api/students'),
        api.get('/api/departments'),
      ]);
      setStudents(stuRes.data);
      setDepartments(deptRes.data);
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

  const openEdit = (student) => {
    setEditing(student);
    setForm({
      name: student.name,
      email: student.email,
      password: '',
      enrollmentNumber: student.enrollmentNumber,
      semester: student.semester,
      departmentId: student.departmentId,
    });
    setFormError('');
    setDialogOpen(true);
  };

  const validate = () => {
    if (!form.name.trim()) return 'Name is required';
    if (!form.email.trim()) return 'Email is required';
    if (!/\S+@\S+\.\S+/.test(form.email)) return 'Please enter a valid email';
    // Password is required only when creating a new student
    if (!editing) {
      if (!form.password.trim()) return 'Password is required';
      if (form.password.trim().length < 8) return 'Password must be at least 8 characters';
    } else if (form.password.trim() && form.password.trim().length < 8) {
      return 'New password must be at least 8 characters';
    }
    if (!form.enrollmentNumber.trim()) return 'Enrollment number is required';
    if (!form.semester) return 'Semester is required';
    if (!form.departmentId) return 'Department is required';
    return null;
  };

  const handleSubmit = async () => {
    const err = validate();
    if (err) { setFormError(err); return; }
    setSubmitting(true);
    try {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        enrollmentNumber: form.enrollmentNumber.trim(),
        semester: parseInt(form.semester, 10),
        departmentId: parseInt(form.departmentId, 10),
      };
      // Only send password if provided (required on create, optional on edit)
      if (form.password.trim()) {
        payload.password = form.password.trim();
      } else if (!editing) {
        // Shouldn't reach here (validate blocks it), but be safe
        payload.password = form.password.trim();
      }
      if (editing) {
        await api.put(`/api/students/${editing.studentId}`, payload);
        setSnackbar({ open: true, message: 'Student updated successfully', severity: 'success' });
      } else {
        await api.post('/api/students', payload);
        setSnackbar({ open: true, message: 'Student created successfully', severity: 'success' });
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
      await api.delete(`/api/students/${deleteTarget.studentId}`);
      setSnackbar({ open: true, message: 'Student deleted successfully', severity: 'success' });
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to delete', severity: 'error' });
      setConfirmOpen(false);
    }
  };

  const downloadStudentTemplate = () => {
    const template = new Blob(
      ['Name,Email,Password,Roll Number,Semester,Department\r\n'],
      { type: 'text/csv;charset=utf-8' }
    );
    const url = URL.createObjectURL(template);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'student-import-template.csv';
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleImportPreview = async () => {
    if (!importFile) {
      setImportError('Choose a CSV or Excel file first.');
      return;
    }
    setImporting(true);
    setImportAction('preview');
    setImportError('');
    try {
      const data = new FormData();
      data.append('file', importFile);
      const response = await api.post('/api/students/import/preview', data);
      setImportPreview(response.data);
    } catch (err) {
      if (err.response?.data?.errors) setImportPreview(err.response.data);
      setImportError(err.response?.data?.message || 'Student file preview failed. Check the file and try again.');
    } finally {
      setImporting(false);
      setImportAction('');
    }
  };

  const handleImport = async () => {
    if (!importFile || !importPreview?.canImport) return;
    setImporting(true);
    setImportAction('commit');
    setImportError('');
    try {
      const data = new FormData();
      data.append('file', importFile);
      const response = await api.post('/api/students/import', data);
      setSnackbar({ open: true, message: response.data.message, severity: 'success' });
      setImportDialogOpen(false);
      setImportFile(null);
      setImportPreview(null);
      fetchData();
    } catch (err) {
      if (err.response?.data?.errors) setImportPreview(err.response.data);
      setImportError(err.response?.data?.message || 'Student import failed. Check the file and try again.');
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
              <PeopleIcon sx={{ fontSize: 30, color: '#FF8F00' }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Student Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '60vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1.5, mb: 3, flexWrap: 'wrap' }}>
              <Button id="import-students-button" variant="outlined" startIcon={<UploadFileIcon />}
                onClick={() => { setImportError(''); setImportPreview(null); setImportFile(null); setImportDialogOpen(true); }}
                sx={{ color: '#FFB74D', borderColor: 'rgba(255,183,77,0.5)', '&:hover': { borderColor: '#FFB74D', backgroundColor: 'rgba(255,183,77,0.08)' } }}>
                Import Students
              </Button>
              <Button id="add-student-button" variant="contained" startIcon={<AddIcon />} onClick={openAdd}
                sx={{ background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)', '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' } }}>
                Add Student
              </Button>
            </Box>

            {loading ? (
              <Loading message="Loading students..." />
            ) : students.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <PeopleIcon sx={{ fontSize: 64, color: '#B0B0B0', opacity: 0.2, mb: 2 }} />
                <Typography variant="h6" sx={{ color: '#B0B0B0' }}>No students found</Typography>
              </Box>
            ) : (
              <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid rgba(255,255,255,0.06)', borderRadius: 3, overflowX: 'auto', backgroundColor: '#242424' }}>
                <Table aria-label="Students table" sx={{ minWidth: 700 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>Email</TableCell>
                      <TableCell>Enrollment No.</TableCell>
                      <TableCell>Semester</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {students.map((stu) => (
                      <TableRow key={stu.studentId}>
                        <TableCell sx={{ fontWeight: 500 }}>{stu.name}</TableCell>
                        <TableCell>{stu.email}</TableCell>
                        <TableCell sx={{ color: '#FFB300', fontWeight: 500 }}>{stu.enrollmentNumber}</TableCell>
                        <TableCell>{stu.semester}</TableCell>
                        <TableCell>{stu.departmentName}</TableCell>
                        <TableCell align="right">
                          <IconButton onClick={() => openEdit(stu)} aria-label={`Edit ${stu.name}`} size="small" sx={{ color: '#FF8F00' }}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton color="error" onClick={() => { setDeleteTarget(stu); setConfirmOpen(true); }} aria-label={`Delete ${stu.name}`} size="small" sx={{ ml: 0.5 }}>
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

        <Dialog open={importDialogOpen} onClose={closeImportDialog} maxWidth="md" fullWidth aria-labelledby="student-import-title">
          <DialogTitle id="student-import-title">Import Students</DialogTitle>
          <DialogContent>
            {importError && <Alert severity="error" sx={{ mb: 2, mt: 1 }}>{importError}</Alert>}
            <Typography sx={{ color: 'text.secondary', mb: 2 }}>
              Upload a CSV, XLS, or XLSX file. Include each student’s name, email, initial password, roll/enrollment number, semester, and an existing department name or ID.
            </Typography>
            <Button onClick={downloadStudentTemplate} sx={{ mb: 2, px: 0 }}>
              Download CSV template
            </Button>
            <Box>
              <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} disabled={importing}>
                Choose file
                <input
                  hidden
                  type="file"
                  accept=".csv,.xls,.xlsx"
                  onChange={(event) => {
                    const file = event.target.files?.[0] || null;
                    event.target.value = '';
                    setImportFile(file);
                    setImportError('');
                    setImportPreview(null);
                  }}
                />
              </Button>
              <Typography component="span" sx={{ ml: 1.5, color: 'text.secondary', overflowWrap: 'anywhere' }}>
                {importFile?.name || 'No file selected'}
              </Typography>
            </Box>
            <Alert severity="info" sx={{ mt: 2 }}>
              Preview checks the whole file first. Duplicate emails or roll numbers are skipped; any invalid row blocks the complete import. Passwords are never shown in the preview and are hashed before saving.
            </Alert>
            <ImportPreviewPanel preview={importPreview} />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={closeImportDialog} color="inherit" variant="outlined" disabled={importing}>Cancel</Button>
            <Button onClick={handleImportPreview} variant="outlined" disabled={importing || !importFile}
              sx={{ borderColor: '#FF8F00', color: '#FFB300' }}>
              {importing && importAction === 'preview' ? 'Checking...' : 'Preview import'}
            </Button>
            <Button onClick={handleImport} variant="contained" disabled={importing || !importFile || !importPreview?.canImport}
              sx={{ background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)', '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' } }}>
              {importing && importAction === 'commit' ? 'Importing...' : 'Confirm import'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Add/Edit Dialog */}
        <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth aria-labelledby="student-dialog-title">
          <DialogTitle id="student-dialog-title">{editing ? 'Edit Student' : 'Add Student'}</DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2, mt: 1, borderRadius: 2, backgroundColor: 'rgba(239, 83, 80, 0.1)', color: '#EF5350' }}>{formError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <TextField name="name" label="Full Name" value={form.name} onChange={handleChange}
                placeholder="e.g. John Doe" slotProps={{ htmlInput: { 'aria-label': 'Full name', maxLength: 100 } }} fullWidth autoFocus />
              <TextField name="email" label="Email" type="email" value={form.email} onChange={handleChange}
                placeholder="e.g. john@example.com" slotProps={{ htmlInput: { 'aria-label': 'Email address', maxLength: 100 } }} fullWidth />
              <TextField name="password" label="Password" type="password" value={form.password} onChange={handleChange}
                placeholder={editing ? 'Enter new password' : '8–72 characters'}
                helperText={editing ? 'Leave blank to keep the current password' : ''}
                slotProps={{ htmlInput: { 'aria-label': 'Password', maxLength: 72, minLength: 8 } }} fullWidth />
              <TextField name="enrollmentNumber" label="Enrollment Number" value={form.enrollmentNumber} onChange={handleChange}
                placeholder="e.g. RBU2024001" slotProps={{ htmlInput: { 'aria-label': 'Enrollment number', maxLength: 255 } }} fullWidth />
              <TextField name="semester" label="Semester" select value={form.semester} onChange={handleChange}
                slotProps={{ htmlInput: { 'aria-label': 'Semester' } }} fullWidth>
                {[1, 2, 3, 4, 5, 6, 7, 8].map((s) => (
                  <MenuItem key={s} value={s}>Semester {s}</MenuItem>
                ))}
              </TextField>
              <TextField name="departmentId" label="Department" select value={form.departmentId} onChange={handleChange}
                slotProps={{ htmlInput: { 'aria-label': 'Department' } }} fullWidth>
                {departments.map((d) => (
                  <MenuItem key={d.departmentId} value={d.departmentId}>{d.departmentName}</MenuItem>
                ))}
              </TextField>
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

        <ConfirmDialog open={confirmOpen} title="Delete Student"
          message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
          onConfirm={handleDelete} onCancel={() => { setConfirmOpen(false); setDeleteTarget(null); }} />

        <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity={snackbar.severity} variant="filled" sx={{ borderRadius: 2 }}>{snackbar.message}</Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
