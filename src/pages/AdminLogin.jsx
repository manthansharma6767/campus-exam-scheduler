import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import LoginIcon from '@mui/icons-material/Login';
import api from '../services/api';

export default function AdminLogin() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (error) setError('');
  };

  const validate = () => {
    if (!form.email.trim()) return 'Email is required';
    if (!/\S+@\S+\.\S+/.test(form.email)) return 'Please enter a valid email';
    if (!form.password.trim()) return 'Password is required';
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
      const response = await api.post('/api/auth/login', {
        email: form.email.trim(),
        password: form.password,
      });
      localStorage.setItem('token', response.data.token);
      navigate('/admin/dashboard');
    } catch (err) {
      if (err.response && (err.response.status === 401 || err.response.status === 403)) {
        setError('Invalid email or password. Please try again.');
      } else if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

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
                Admin Login
              </Typography>
              <Typography sx={{ mt: 0.5, color: '#B0B0B0' }}>
                Sign in to manage the exam scheduler
              </Typography>
            </Box>

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
                id="admin-email-input"
                name="email"
                label="Email Address"
                type="email"
                fullWidth
                value={form.email}
                onChange={handleChange}
                placeholder="admin@gmail.com"
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
                id="admin-password-input"
                name="password"
                label="Password"
                type={showPassword ? 'text' : 'password'}
                fullWidth
                value={form.password}
                onChange={handleChange}
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
                  htmlInput: { 'aria-label': 'Password' },
                }}
                sx={{ mb: 3 }}
                autoComplete="current-password"
              />
              <Button
                id="admin-login-button"
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={loading}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <LoginIcon />}
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
                {loading ? 'Signing in...' : 'Sign In'}
              </Button>
            </Box>
          </Paper>
        </Container>
      </Box>
    </Fade>
  );
}
