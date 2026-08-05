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
import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import Fade from '@mui/material/Fade';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../services/api';

export default function Departments() {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [formValue, setFormValue] = useState('');
  const [formError, setFormError] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [submitting, setSubmitting] = useState(false);

  const fetchDepartments = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/departments');
      setDepartments(res.data);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load departments', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDepartments(); }, []);

  const openAdd = () => {
    setEditing(null);
    setFormValue('');
    setFormError('');
    setDialogOpen(true);
  };

  const openEdit = (dept) => {
    setEditing(dept);
    setFormValue(dept.departmentName);
    setFormError('');
    setDialogOpen(true);
  };

  const handleSubmit = async () => {
    if (!formValue.trim()) {
      setFormError('Department name is required');
      return;
    }
    if (formValue.trim().length < 2) {
      setFormError('Department name must be at least 2 characters');
      return;
    }
    setSubmitting(true);
    try {
      if (editing) {
        await api.put(`/api/departments/${editing.departmentId}`, {
          departmentName: formValue.trim(),
        });
        setSnackbar({ open: true, message: 'Department updated successfully', severity: 'success' });
      } else {
        await api.post('/api/departments', {
          departmentName: formValue.trim(),
        });
        setSnackbar({ open: true, message: 'Department created successfully', severity: 'success' });
      }
      setDialogOpen(false);
      fetchDepartments();
    } catch (err) {
      const msg = err.response?.data?.message || 'An error occurred';
      setFormError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    try {
      await api.delete(`/api/departments/${deleteTarget.departmentId}`);
      setSnackbar({ open: true, message: 'Department deleted successfully', severity: 'success' });
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchDepartments();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete department';
      setSnackbar({ open: true, message: msg, severity: 'error' });
      setConfirmOpen(false);
    }
  };

  return (
    <Fade in timeout={600}>
      <Box>
        {/* Header */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)',
            color: 'white',
            py: { xs: 3, md: 4 },
          }}
        >
          <Container maxWidth="lg">
            <Button
              component={Link}
              to="/admin/dashboard"
              startIcon={<ArrowBackIcon />}
              sx={{ color: 'rgba(255,255,255,0.7)', mb: 1, '&:hover': { color: '#FF8F00', backgroundColor: 'rgba(230, 81, 0, 0.08)' } }}
            >
              Back to Dashboard
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <AccountBalanceIcon sx={{ fontSize: 30, color: '#FF8F00' }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Department Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '60vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
            {/* Toolbar */}
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
              <Button
                id="add-department-button"
                variant="contained"
                startIcon={<AddIcon />}
                onClick={openAdd}
                sx={{
                  background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                  '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' },
                }}
              >
                Add Department
              </Button>
            </Box>

            {/* Table */}
            {loading ? (
              <Loading message="Loading departments..." />
            ) : departments.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <AccountBalanceIcon sx={{ fontSize: 64, color: '#B0B0B0', opacity: 0.2, mb: 2 }} />
                <Typography variant="h6" sx={{ color: '#B0B0B0' }}>No departments found</Typography>
                <Typography sx={{ color: '#888', mb: 2 }}>Click "Add Department" to create your first department.</Typography>
              </Box>
            ) : (
              <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid rgba(255,255,255,0.06)', borderRadius: 3, backgroundColor: '#242424' }}>
                <Table aria-label="Departments table">
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      <TableCell>Department Name</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {departments.map((dept) => (
                      <TableRow key={dept.departmentId}>
                        <TableCell sx={{ fontWeight: 500, color: '#FFB300' }}>{dept.departmentId}</TableCell>
                        <TableCell>{dept.departmentName}</TableCell>
                        <TableCell align="right">
                          <IconButton
                            onClick={() => openEdit(dept)}
                            aria-label={`Edit ${dept.departmentName}`}
                            size="small"
                            sx={{ color: '#FF8F00' }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            color="error"
                            onClick={() => { setDeleteTarget(dept); setConfirmOpen(true); }}
                            aria-label={`Delete ${dept.departmentName}`}
                            size="small"
                            sx={{ ml: 0.5 }}
                          >
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

        {/* Add/Edit Dialog */}
        <Dialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          maxWidth="sm"
          fullWidth
          aria-labelledby="department-dialog-title"
        >
          <DialogTitle id="department-dialog-title">
            {editing ? 'Edit Department' : 'Add Department'}
          </DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2, mt: 1, borderRadius: 2, backgroundColor: 'rgba(239, 83, 80, 0.1)', color: '#EF5350' }}>{formError}</Alert>}
            <TextField
              id="department-name-input"
              label="Department Name"
              fullWidth
              value={formValue}
              onChange={(e) => { setFormValue(e.target.value); setFormError(''); }}
              placeholder="e.g. Computer Science"
              sx={{ mt: 1 }}
              slotProps={{ htmlInput: { 'aria-label': 'Department name', maxLength: 100 } }}
              autoFocus
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setDialogOpen(false)} color="inherit" variant="outlined">Cancel</Button>
            <Button
              onClick={handleSubmit}
              variant="contained"
              disabled={submitting}
              sx={{
                background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' },
              }}
            >
              {submitting ? 'Saving...' : editing ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Confirm Delete */}
        <ConfirmDialog
          open={confirmOpen}
          title="Delete Department"
          message={`Are you sure you want to delete "${deleteTarget?.departmentName}"? This action cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => { setConfirmOpen(false); setDeleteTarget(null); }}
        />

        {/* Snackbar */}
        <Snackbar
          open={snackbar.open}
          autoHideDuration={4000}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity={snackbar.severity} variant="filled" sx={{ borderRadius: 2 }}>
            {snackbar.message}
          </Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
