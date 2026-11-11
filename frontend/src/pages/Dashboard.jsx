import { useState } from 'react';
import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Button from '@mui/material/Button';
import Fade from '@mui/material/Fade';
import Alert from '@mui/material/Alert';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import Snackbar from '@mui/material/Snackbar';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import PeopleIcon from '@mui/icons-material/People';
import QuizIcon from '@mui/icons-material/Quiz';
import ChairIcon from '@mui/icons-material/Chair';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LockResetIcon from '@mui/icons-material/LockReset';
import api from '../services/api';

const dashboardCards = [
  {
    title: 'Departments',
    description: 'Manage academic departments. Add, edit, or remove department records.',
    icon: AccountBalanceIcon,
    path: '/admin/departments',
    color: '#E65100',
  },
  {
    title: 'Subjects',
    description: 'Manage course subjects. Configure subject codes, names, and semester assignments.',
    icon: MenuBookIcon,
    path: '/admin/subjects',
    color: '#FF8F00',
  },
  {
    title: 'Students',
    description: 'Manage student records. Handle enrollment, department assignments, and credentials.',
    icon: PeopleIcon,
    path: '/admin/students',
    color: '#FFB300',
  },
  {
    title: 'Exams',
    description: 'Schedule and manage examinations. Set dates, times, rooms, and building assignments.',
    icon: QuizIcon,
    path: '/admin/exams',
    color: '#BF360C',
  },
  {
    title: 'Seating Plans',
    description: 'Import room seating charts and let students look up their room and seat by roll number.',
    icon: ChairIcon,
    path: '/admin/seating',
    color: '#7E57C2',
  },
];

const emptyPasswordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };

export default function Dashboard() {
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState(emptyPasswordForm);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const openPasswordDialog = () => {
    setPasswordForm(emptyPasswordForm);
    setPasswordError('');
    setPasswordDialogOpen(true);
  };

  const handlePasswordChange = (event) => {
    setPasswordForm({ ...passwordForm, [event.target.name]: event.target.value });
    setPasswordError('');
  };

  const submitPasswordChange = async () => {
    if (!passwordForm.currentPassword) {
      setPasswordError('Current password is required.');
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters.');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New passwords do not match.');
      return;
    }
    setPasswordSaving(true);
    setPasswordError('');
    try {
      await api.put('/api/auth/password', {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordDialogOpen(false);
      setPasswordForm(emptyPasswordForm);
      setSnackbar({
        open: true,
        message: 'Password changed successfully.',
        severity: 'success',
      });
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Could not change the password.');
    } finally {
      setPasswordSaving(false);
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
            py: { xs: 4, md: 5 },
          }}
        >
          <Container maxWidth="lg">
            <Box
              sx={{
                display: 'flex',
                alignItems: { xs: 'flex-start', sm: 'center' },
                justifyContent: 'space-between',
                gap: 2,
                flexWrap: 'wrap',
              }}
            >
              <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
                  <DashboardIcon sx={{ fontSize: 32, color: '#FF8F00' }} />
                  <Typography
                    variant="h3"
                    sx={{
                      fontWeight: 800,
                      fontSize: { xs: '1.5rem', md: '2.2rem' },
                      fontFamily: '"Playfair Display", "Georgia", serif',
                      fontStyle: 'italic',
                    }}
                  >
                    Admin Dashboard
                  </Typography>
                </Box>
                <Typography variant="body1" sx={{ opacity: 0.7 }}>
                  Manage your university examination system
                </Typography>
              </Box>
              <Button
                variant="outlined"
                startIcon={<LockResetIcon />}
                onClick={openPasswordDialog}
                sx={{
                  color: '#FFB300',
                  borderColor: 'rgba(255, 179, 0, 0.5)',
                  '&:hover': {
                    borderColor: '#FFB300',
                    backgroundColor: 'rgba(255, 179, 0, 0.08)',
                  },
                }}
              >
                Change password
              </Button>
            </Box>
          </Container>
        </Box>

        {/* Cards Grid */}
        <Box sx={{ background: '#1A1A1A', minHeight: '60vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 4, md: 5 } }}>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: {
                  xs: '1fr',
                  sm: 'repeat(2, 1fr)',
                  lg: 'repeat(3, 1fr)',
                },
                gap: 3,
              }}
            >
              {dashboardCards.map((card) => {
                const IconComp = card.icon;
                return (
                  <Card
                    key={card.title}
                    sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      position: 'relative',
                      overflow: 'visible',
                      backgroundColor: '#2A2A2A',
                      border: '1px solid rgba(255,255,255,0.06)',
                      '&:hover': {
                        borderColor: `${card.color}40`,
                        boxShadow: `0 8px 30px ${card.color}15`,
                      },
                    }}
                  >
                    {/* Color accent */}
                    <Box
                      sx={{
                        height: 4,
                        backgroundColor: card.color,
                        borderRadius: '16px 16px 0 0',
                      }}
                    />
                    <CardContent sx={{ p: 3, flexGrow: 1 }}>
                      <Box
                        sx={{
                          width: 52,
                          height: 52,
                          borderRadius: 2.5,
                          backgroundColor: `${card.color}18`,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          mb: 2,
                        }}
                      >
                        <IconComp sx={{ fontSize: 28, color: card.color }} />
                      </Box>
                      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1, color: '#FFFFFF' }}>
                        {card.title}
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{ lineHeight: 1.6, color: '#B0B0B0' }}
                      >
                        {card.description}
                      </Typography>
                    </CardContent>
                    <CardActions sx={{ px: 3, pb: 2.5 }}>
                      <Button
                        component={Link}
                        to={card.path}
                        variant="contained"
                        endIcon={<ArrowForwardIcon />}
                        fullWidth
                        sx={{
                          backgroundColor: card.color,
                          color: '#FFFFFF',
                          '&:hover': {
                            backgroundColor: card.color,
                            filter: 'brightness(1.15)',
                            boxShadow: `0 4px 16px ${card.color}50`,
                          },
                        }}
                      >
                        Manage
                      </Button>
                    </CardActions>
                  </Card>
                );
              })}
            </Box>
          </Container>
        </Box>

        <Dialog
          open={passwordDialogOpen}
          onClose={() => { if (!passwordSaving) setPasswordDialogOpen(false); }}
          maxWidth="sm"
          fullWidth
          aria-labelledby="change-password-title"
        >
          <DialogTitle id="change-password-title">Change Admin Password</DialogTitle>
          <DialogContent>
            <Typography sx={{ color: 'text.secondary', mb: 2 }}>
              Use a strong password of at least 8 characters. This changes the
              password for the signed-in administrator only.
            </Typography>
            {passwordError && <Alert severity="error" sx={{ mb: 2 }}>{passwordError}</Alert>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
              <TextField
                name="currentPassword"
                label="Current password"
                type="password"
                value={passwordForm.currentPassword}
                onChange={handlePasswordChange}
                autoComplete="current-password"
                slotProps={{ htmlInput: { 'aria-label': 'Current password', maxLength: 72 } }}
                fullWidth
                autoFocus
              />
              <TextField
                name="newPassword"
                label="New password"
                type="password"
                value={passwordForm.newPassword}
                onChange={handlePasswordChange}
                autoComplete="new-password"
                slotProps={{ htmlInput: { 'aria-label': 'New password', maxLength: 72, minLength: 8 } }}
                fullWidth
              />
              <TextField
                name="confirmPassword"
                label="Confirm new password"
                type="password"
                value={passwordForm.confirmPassword}
                onChange={handlePasswordChange}
                autoComplete="new-password"
                slotProps={{ htmlInput: { 'aria-label': 'Confirm new password', maxLength: 72 } }}
                fullWidth
              />
            </Box>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button
              onClick={() => setPasswordDialogOpen(false)}
              color="inherit"
              variant="outlined"
              disabled={passwordSaving}
            >
              Cancel
            </Button>
            <Button
              onClick={submitPasswordChange}
              variant="contained"
              disabled={passwordSaving}
              sx={{
                background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                '&:hover': { background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)' },
              }}
            >
              {passwordSaving ? 'Saving...' : 'Change password'}
            </Button>
          </DialogActions>
        </Dialog>

        <Snackbar
          open={snackbar.open}
          autoHideDuration={4000}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert
            severity={snackbar.severity}
            variant="filled"
            sx={{ borderRadius: 2 }}
          >
            {snackbar.message}
          </Alert>
        </Snackbar>
      </Box>
    </Fade>
  );
}
