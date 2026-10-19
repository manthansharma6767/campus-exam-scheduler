import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Alert from '@mui/material/Alert';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';
import Fade from '@mui/material/Fade';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import PersonIcon from '@mui/icons-material/Person';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import api from '../services/api';

export default function AdminSetup() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '', setupToken: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (error) setError('');
  };

  const validate = () => {
    if (!form.name.trim()) return 'Name is required';
    if (!form.email.trim()) return 'Email is required';
    if (!/\S+@\S+\.\S+/.test(form.email)) return 'Please enter a valid email';
    if (!form.password) return 'Password is required';
    if (form.password.length < 8) return 'Password must be at least 8 characters';
    if (form.password.length > 72) return 'Password must be no more than 72 characters';
    if (form.password !== form.confirmPassword) return 'Passwords do not match';
    if (!form.setupToken.trim()) return 'Setup token is required';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setLoading(true);
    setError('');
    try {
      await api.post('/api/auth/register', {
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      }, {
        headers: { 'X-Admin-Setup-Token': form.setupToken.trim() },
      });
      setSuccess(true);
    } catch (err) {
      if (err.response?.status === 400 && err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.response?.status === 409) {
        setError(err.response?.data?.message || 'An administrator account already exists. Please log in instead.');
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <Fade in timeout={600}>
        <Box
          sx={{
            minHeight: 'calc(100vh - 200px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            py: { xs: 4, md: 6 },
            px: 2,
            background: 'linear-gradient(180deg, #1A2E1A 0%, #1A1A1A 50%)',
          }}
        >
          <Container maxWidth="sm">
            <Paper
              elevation={0}
              sx={{
                p: { xs: 3, sm: 5 },
                borderRadius: 4,
                border: '1px solid rgba(76, 175, 80, 0.2)',
                backgroundColor: '#242424',
                boxShadow: '0 8px 40px rgba(0, 0, 0, 0.4)',
                textAlign: 'center',
              }}
            >
              <CheckCircleOutlinedIcon sx={{ fontSize: 64, color: '#4CAF50', mb: 2 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, color: '#FFFFFF', mb: 1 }}>
                Setup Complete!
              </Typography>
              <Typography sx={{ color: '#B0B0B0', mb: 3 }}>
                Your administrator account has been created. You can now log in and manage the exam scheduler.
              </Typography>
              <Button
                id="setup-goto-login-button"
                variant="contained"
                fullWidth
                size="large"
                onClick={() => navigate('/admin/login')}
                sx={{
                  py: 1.5,
                  background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                  },
                }}
              >
                Go to Login
              </Button>
            </Paper>
          </Container>
        </Box>
      </Fade>
    );
  }

  return (
    <Fade in timeout={600}>
      <Box
        sx={{
          minHeight: 'calc(100vh - 200px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          py: { xs: 4, md: 6 },
          px: 2,
          background: 'linear-gradient(180deg, #2D1500 0%, #1A1A1A 50%, #1A1A1A 100%)',
        }}
      >
        <Container maxWidth="sm">
          <Paper
            elevation={0}
            sx={{
              p: { xs: 3, sm: 5 },
              borderRadius: 4,
              border: '1px solid rgba(255,255,255,0.08)',
              backgroundColor: '#242424',
              boxShadow: '0 8px 40px rgba(0, 0, 0, 0.4)',
            }}
          >
            {/* Header */}
            <Box sx={{ textAlign: 'center', mb: 4 }}>
              <Box
                sx={{
                  width: 64,
                  height: 64,
                  borderRadius: 3,
                  background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  mx: 'auto',
                  mb: 2,
                  boxShadow: '0 4px 16px rgba(230, 81, 0, 0.35)',
                }}
              >
                <AdminPanelSettingsIcon sx={{ fontSize: 32, color: 'white' }} />
              </Box>
              <Typography variant="h4" sx={{ fontWeight: 700, color: '#FFFFFF' }}>
                First-time Setup
              </Typography>
              <Typography sx={{ mt: 0.5, color: '#B0B0B0' }}>
                Create the initial administrator account
              </Typography>
            </Box>

            <Alert
              severity="info"
              sx={{
                mb: 3,
                backgroundColor: 'rgba(255, 143, 0, 0.08)',
                color: '#FFB300',
                border: '1px solid rgba(255, 143, 0, 0.2)',
                '& .MuiAlert-icon': { color: '#FFB300' },
              }}
            >
              Enter the one-time setup token configured by the site operator. This page only works before the first administrator is created.
            </Alert>

            {/* Error Alert */}
            {error && (
              <Alert
                severity="error"
                sx={{
                  mb: 3,
                  borderRadius: 2,
                  backgroundColor: 'rgba(239, 83, 80, 0.1)',
                  color: '#EF5350',
                  border: '1px solid rgba(239, 83, 80, 0.2)',
                }}
              >
                {error}
              </Alert>
            )}

            {/* Form */}
            <Box component="form" onSubmit={handleSubmit} noValidate>
              <TextField
                id="setup-token-input"
                name="setupToken"
                label="One-time Setup Token"
                type="password"
                fullWidth
                value={form.setupToken}
                onChange={handleChange}
                autoComplete="off"
                slotProps={{ htmlInput: { 'aria-label': 'One-time setup token' } }}
                sx={{ mb: 2.5 }}
              />
              <TextField
                id="setup-name-input"
                name="name"
                label="Full Name"
                fullWidth
                value={form.name}
                onChange={handleChange}
                placeholder="e.g. Admin User"
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <PersonIcon sx={{ color: '#B0B0B0' }} />
                      </InputAdornment>
                    ),
                  },
                  htmlInput: { 'aria-label': 'Full name', maxLength: 100 },
                }}
                sx={{ mb: 2.5 }}
              />
              <TextField
                id="setup-email-input"
                name="email"
                label="Email Address"
                type="email"
                fullWidth
                value={form.email}
                onChange={handleChange}
                placeholder="admin@example.com"
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <EmailIcon sx={{ color: '#B0B0B0' }} />
                      </InputAdornment>
                    ),
                  },
                  htmlInput: { 'aria-label': 'Email address' },
                }}
                sx={{ mb: 2.5 }}
                autoComplete="email"
              />
              <TextField
                id="setup-password-input"
                name="password"
                label="Password"
                type={showPassword ? 'text' : 'password'}
                fullWidth
                value={form.password}
                onChange={handleChange}
                helperText="Minimum 8 characters"
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <LockIcon sx={{ color: '#B0B0B0' }} />
                      </InputAdornment>
                    ),
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowPassword(!showPassword)}
                          edge="end"
                          aria-label={showPassword ? 'Hide password' : 'Show password'}
                          sx={{ color: '#B0B0B0' }}
                        >
                          {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  },
                  htmlInput: { 'aria-label': 'Password', minLength: 8, maxLength: 72 },
                }}
                sx={{ mb: 2.5 }}
              />
              <TextField
                id="setup-confirm-password-input"
                name="confirmPassword"
                label="Confirm Password"
                type={showPassword ? 'text' : 'password'}
                fullWidth
                value={form.confirmPassword}
                onChange={handleChange}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <LockIcon sx={{ color: '#B0B0B0' }} />
                      </InputAdornment>
                    ),
                  },
                  htmlInput: { 'aria-label': 'Confirm password', maxLength: 72 },
                }}
                sx={{ mb: 3 }}
              />
              <Button
                id="setup-submit-button"
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={loading}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <AdminPanelSettingsIcon />}
                sx={{
                  py: 1.5,
                  fontSize: '1rem',
                  background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                  boxShadow: '0 4px 16px rgba(230, 81, 0, 0.35)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                    boxShadow: '0 6px 24px rgba(230, 81, 0, 0.5)',
                  },
                  '&.Mui-disabled': {
                    backgroundColor: 'rgba(230, 81, 0, 0.3)',
                    color: 'rgba(255,255,255,0.5)',
                  },
                }}
              >
                {loading ? 'Creating account...' : 'Create Administrator Account'}
              </Button>
            </Box>

            <Divider sx={{ my: 3, borderColor: 'rgba(255,255,255,0.08)' }} />
            <Typography variant="body2" sx={{ textAlign: 'center', color: '#B0B0B0' }}>
              Already have an account?{' '}
              <Box
                component={Link}
                to="/admin/login"
                sx={{ color: '#FF8F00', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
              >
                Sign in here
              </Box>
            </Typography>
          </Paper>
        </Container>
      </Box>
    </Fade>
  );
}
