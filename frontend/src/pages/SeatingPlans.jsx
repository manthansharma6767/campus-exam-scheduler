import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Fade from '@mui/material/Fade';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Snackbar from '@mui/material/Snackbar';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ChairIcon from '@mui/icons-material/Chair';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DeleteIcon from '@mui/icons-material/Delete';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../services/api';

function formatImportedAt(value) {
  if (!value) return '—';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export default function SeatingPlans() {
  const fileInput = useRef(null);
  const [plans, setPlans] = useState([]);
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState({ open: false, message: '' });
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchPlans = async () => {
    try {
      const { data } = await api.get('/api/seating/plans');
      setPlans(data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not load seating plans.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPlans(); }, []);

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!file) {
      setError('Choose a seating plan PDF first.');
      return;
    }
    setUploading(true);
    setError('');
    const body = new FormData();
    body.append('file', file);
    try {
      const { data } = await api.post('/api/seating/plans/import', body, {
        params: title.trim() ? { title: title.trim() } : undefined,
      });
      setNotice({ open: true, message: data.message });
      setFile(null);
      setTitle('');
      if (fileInput.current) fileInput.current.value = '';
      await fetchPlans();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The seating plan could not be imported.');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await api.delete(`/api/seating/plans/${deleteTarget.seatingPlanId}`);
      setNotice({ open: true, message: 'Seating plan deleted.' });
      setDeleteTarget(null);
      await fetchPlans();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not delete this seating plan.');
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Fade in timeout={500}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)', color: 'white', py: { xs: 3, md: 4 } }}>
          <Container maxWidth="lg">
            <Button component={Link} to="/admin/dashboard" startIcon={<ArrowBackIcon />} sx={{ color: 'rgba(255,255,255,0.7)', mb: 1 }}>
              Back to Dashboard
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <ChairIcon sx={{ color: '#FF8F00', fontSize: 30 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: '1.3rem', md: '1.75rem' } }}>
                Seating Plan Management
              </Typography>
            </Box>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '65vh', py: { xs: 3, md: 4 } }}>
          <Container maxWidth="lg">
            <Paper component="form" onSubmit={handleUpload} sx={{ p: { xs: 2.5, md: 3 }, mb: 3, backgroundColor: '#242424', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>Import a seating arrangement</Typography>
              <Typography variant="body2" sx={{ color: '#B0B0B0', mb: 2.5 }}>
                Upload a text-based PDF containing room headings and roll numbers such as A1-16. The uploaded chart is saved to the database for student lookup.
              </Typography>
              {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 2fr auto' }, gap: 2, alignItems: 'center' }}>
                <TextField
                  label="Plan name (optional)"
                  placeholder="Uses the session title from the PDF"
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  inputProps={{ maxLength: 200 }}
                  fullWidth
                />
                <Button component="label" variant="outlined" startIcon={<CloudUploadIcon />} sx={{ py: 1.5, justifyContent: 'flex-start', overflow: 'hidden' }}>
                  <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {file?.name || 'Choose PDF file'}
                  </Box>
                  <input
                    ref={fileInput}
                    type="file"
                    hidden
                    accept=".pdf,application/pdf"
                    onChange={(event) => { setFile(event.target.files?.[0] || null); setError(''); }}
                  />
                </Button>
                <Button type="submit" variant="contained" disabled={uploading || !file} sx={{ py: 1.5, minWidth: 160 }}>
                  {uploading ? 'Importing…' : 'Import seating plan'}
                </Button>
              </Box>
            </Paper>

            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>Imported plans</Typography>
              <Button component={Link} to="/seating" size="small">Open student lookup</Button>
            </Box>

            {loading ? <Loading message="Loading seating plans…" /> : plans.length === 0 ? (
              <Paper sx={{ p: 4, textAlign: 'center', backgroundColor: '#242424', color: '#B0B0B0' }}>
                No seating plans have been imported yet.
              </Paper>
            ) : (
              <TableContainer component={Paper} sx={{ backgroundColor: '#242424', border: '1px solid rgba(255,255,255,0.06)', borderRadius: 3 }}>
                <Table aria-label="Imported seating plans" sx={{ minWidth: 720 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Plan</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell>Semester</TableCell>
                      <TableCell align="right">Seats</TableCell>
                      <TableCell align="right">Rooms</TableCell>
                      <TableCell>Imported</TableCell>
                      <TableCell align="right">Action</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {plans.map((plan) => (
                      <TableRow key={plan.seatingPlanId}>
                        <TableCell sx={{ fontWeight: 600 }}>{plan.title}</TableCell>
                        <TableCell>{plan.departmentName}</TableCell>
                        <TableCell>{plan.semester ? `Semester ${plan.semester}` : '—'}</TableCell>
                        <TableCell align="right">{plan.seatCount}</TableCell>
                        <TableCell align="right">{plan.roomCount}</TableCell>
                        <TableCell>{formatImportedAt(plan.importedAt)}</TableCell>
                        <TableCell align="right">
                          <IconButton aria-label={`Delete ${plan.title}`} color="error" onClick={() => setDeleteTarget(plan)}>
                            <DeleteIcon />
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

        <ConfirmDialog
          open={Boolean(deleteTarget)}
          title="Delete seating plan"
          message={`Delete “${deleteTarget?.title || ''}” and all of its seat assignments?`}
          onConfirm={handleDelete}
          onCancel={() => !deleting && setDeleteTarget(null)}
        />
        <Snackbar open={notice.open} autoHideDuration={4000} onClose={() => setNotice({ ...notice, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
          <Alert severity="success" variant="filled" sx={{ borderRadius: 2 }}>{notice.message}</Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
