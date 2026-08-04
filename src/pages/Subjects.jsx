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
import MenuBookIcon from '@mui/icons-material/MenuBook';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../services/api';

const emptyForm = { subjectCode: '', subjectName: '', semester: '', departmentId: '' };

export default function Subjects() {
  const [subjects, setSubjects] = useState([]);
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
      const [subRes, deptRes] = await Promise.all([
        api.get('/api/subjects'),
        api.get('/api/departments'),
      ]);
      setSubjects(subRes.data);
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

  const openEdit = (subject) => {
    setEditing(subject);
    setForm({
      subjectCode: subject.subjectCode,
      subjectName: subject.subjectName,
      semester: subject.semester,
      departmentId: subject.departmentId,
    });
    setFormError('');
    setDialogOpen(true);
  };

  const validate = () => {
    if (!form.subjectCode.trim()) return 'Subject code is required';
    if (!form.subjectName.trim()) return 'Subject name is required';
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
        subjectCode: form.subjectCode.trim(),
        subjectName: form.subjectName.trim(),
        semester: parseInt(form.semester, 10),
        departmentId: parseInt(form.departmentId, 10),
      };
      if (editing) {
        await api.put(`/api/subjects/${editing.subjectId}`, payload);
        setSnackbar({ open: true, message: 'Subject updated successfully', severity: 'success' });
      } else {
        await api.post('/api/subjects', payload);
        setSnackbar({ open: true, message: 'Subject created successfully', severity: 'success' });
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
      await api.delete(`/api/subjects/${deleteTarget.subjectId}`);
      setSnackbar({ open: true, message: 'Subject deleted successfully', severity: 'success' });
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
              <MenuBookIcon sx={{ fontSize: 30 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Subject Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
            <Button id="add-subject-button" variant="contained" color="secondary" startIcon={<AddIcon />} onClick={openAdd}>
              Add Subject
            </Button>
          </Box>

          {loading ? (
            <Loading message="Loading subjects..." />
          ) : subjects.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <MenuBookIcon sx={{ fontSize: 64, color: 'text.secondary', opacity: 0.3, mb: 2 }} />
              <Typography variant="h6" color="text.secondary">No subjects found</Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, overflowX: 'auto' }}>
              <Table aria-label="Subjects table" sx={{ minWidth: 600 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Code</TableCell>
                    <TableCell>Subject Name</TableCell>
                    <TableCell>Semester</TableCell>
                    <TableCell>Department</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {subjects.map((sub) => (
                    <TableRow key={sub.subjectId}>
                      <TableCell>
                        <Chip label={sub.subjectCode} size="small" variant="outlined" color="secondary" />
                      </TableCell>
                      <TableCell sx={{ fontWeight: 500 }}>{sub.subjectName}</TableCell>
                      <TableCell>{sub.semester}</TableCell>
                      <TableCell>{sub.departmentName}</TableCell>
                      <TableCell align="right">
                        <IconButton color="secondary" onClick={() => openEdit(sub)} aria-label={`Edit ${sub.subjectName}`} size="small">
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton color="error" onClick={() => { setDeleteTarget(sub); setConfirmOpen(true); }} aria-label={`Delete ${sub.subjectName}`} size="small" sx={{ ml: 0.5 }}>
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
        <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth aria-labelledby="subject-dialog-title">
          <DialogTitle id="subject-dialog-title">{editing ? 'Edit Subject' : 'Add Subject'}</DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2, mt: 1, borderRadius: 2 }}>{formError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <TextField name="subjectCode" label="Subject Code" value={form.subjectCode} onChange={handleChange}
                placeholder="e.g. CS301" slotProps={{ htmlInput: { 'aria-label': 'Subject code', maxLength: 20 } }} fullWidth autoFocus />
              <TextField name="subjectName" label="Subject Name" value={form.subjectName} onChange={handleChange}
                placeholder="e.g. Operating Systems" slotProps={{ htmlInput: { 'aria-label': 'Subject name', maxLength: 100 } }} fullWidth />
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

        <ConfirmDialog open={confirmOpen} title="Delete Subject"
          message={`Are you sure you want to delete "${deleteTarget?.subjectName}"? This action cannot be undone.`}
          onConfirm={handleDelete} onCancel={() => { setConfirmOpen(false); setDeleteTarget(null); }} />

        <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity={snackbar.severity} variant="filled" sx={{ borderRadius: 2 }}>{snackbar.message}</Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
