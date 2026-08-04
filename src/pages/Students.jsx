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
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
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
    if (!form.password.trim()) return 'Password is required';
    if (form.password.trim().length < 8) return 'Password must be at least 8 characters';
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
        password: form.password.trim(),
        enrollmentNumber: form.enrollmentNumber.trim(),
        semester: parseInt(form.semester, 10),
        departmentId: parseInt(form.departmentId, 10),
      };
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

  return (
    <Fade in timeout={600}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #0A1929 0%, #1A3A5C 100%)', color: 'white', py: { xs: 3, md: 4 } }}>
          <Container maxWidth="lg">
            <Button component={Link} to="/admin/dashboard" startIcon={<ArrowBackIcon />}
              sx={{ color: 'rgba(255,255,255,0.8)', mb: 1, '&:hover': { color: 'white', backgroundColor: 'rgba(255,255,255,0.1)' } }}>
              Back to Dashboard
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <PeopleIcon sx={{ fontSize: 30 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Student Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
            <Button id="add-student-button" variant="contained" color="secondary" startIcon={<AddIcon />} onClick={openAdd}>
              Add Student
            </Button>
          </Box>

          {loading ? (
            <Loading message="Loading students..." />
          ) : students.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', opacity: 0.3, mb: 2 }} />
              <Typography variant="h6" color="text.secondary">No students found</Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, overflowX: 'auto' }}>
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
                      <TableCell>{stu.enrollmentNumber}</TableCell>
                      <TableCell>{stu.semester}</TableCell>
                      <TableCell>{stu.departmentName}</TableCell>
                      <TableCell align="right">
                        <IconButton color="secondary" onClick={() => openEdit(stu)} aria-label={`Edit ${stu.name}`} size="small">
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

        {/* Add/Edit Dialog */}
        <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth aria-labelledby="student-dialog-title">
          <DialogTitle id="student-dialog-title">{editing ? 'Edit Student' : 'Add Student'}</DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2, mt: 1, borderRadius: 2 }}>{formError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <TextField name="name" label="Full Name" value={form.name} onChange={handleChange}
                placeholder="e.g. John Doe" slotProps={{ htmlInput: { 'aria-label': 'Full name', maxLength: 100 } }} fullWidth autoFocus />
              <TextField name="email" label="Email" type="email" value={form.email} onChange={handleChange}
                placeholder="e.g. john@example.com" slotProps={{ htmlInput: { 'aria-label': 'Email address', maxLength: 100 } }} fullWidth />
              <TextField name="password" label="Password" type="password" value={form.password} onChange={handleChange}
                placeholder={editing ? 'Enter new password' : 'Minimum 8 characters'}
                helperText={editing ? 'A new password must be provided when updating' : ''}
                slotProps={{ htmlInput: { 'aria-label': 'Password', maxLength: 255, minLength: 8 } }} fullWidth />
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
            <Button onClick={handleSubmit} variant="contained" color="secondary" disabled={submitting}>
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
