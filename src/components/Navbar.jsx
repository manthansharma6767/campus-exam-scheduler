import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import ListItemIcon from '@mui/material/ListItemIcon';
import Divider from '@mui/material/Divider';
import useMediaQuery from '@mui/material/useMediaQuery';
import { useTheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import HomeIcon from '@mui/icons-material/Home';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LogoutIcon from '@mui/icons-material/Logout';
import LoginIcon from '@mui/icons-material/Login';
import rbuLogo from '../assets/rbu-logo.png';

export default function Navbar() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isLoggedIn = !!localStorage.getItem('token');
  const isAdminRoute = location.pathname.startsWith('/admin');

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/admin/login');
    setDrawerOpen(false);
  };

  const navItems = [
    { label: 'Home', path: '/', icon: <HomeIcon /> },
    ...(isLoggedIn
      ? [
          { label: 'Dashboard', path: '/admin/dashboard', icon: <DashboardIcon /> },
        ]
      : [
          { label: 'Admin', path: '/admin/login', icon: <AdminPanelSettingsIcon /> },
        ]),
  ];

  const drawer = (
    <Box sx={{ width: 280 }} role="navigation" aria-label="Main navigation">
      <Box
        sx={{
          p: 2.5,
          background: 'linear-gradient(135deg, #0A1929 0%, #1A3A5C 100%)',
          color: 'white',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box component="img" src={rbuLogo} alt="RBU Logo" sx={{ height: 40, width: 'auto' }} />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
              RBU
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.85 }}>
              Exam Scheduler
            </Typography>
          </Box>
        </Box>
      </Box>
      <Divider />
      <List sx={{ pt: 1 }}>
        {navItems.map((item) => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton
              component={Link}
              to={item.path}
              selected={location.pathname === item.path}
              onClick={() => setDrawerOpen(false)}
              sx={{
                mx: 1,
                borderRadius: 2,
                mb: 0.5,
                '&.Mui-selected': {
                  backgroundColor: 'rgba(25, 118, 210, 0.08)',
                  '&:hover': {
                    backgroundColor: 'rgba(25, 118, 210, 0.12)',
                  },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40, color: location.pathname === item.path ? 'secondary.main' : 'text.secondary' }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                slotProps={{
                  primary: {
                    fontWeight: location.pathname === item.path ? 600 : 400,
                  },
                }}
              />
            </ListItemButton>
          </ListItem>
        ))}
        {isLoggedIn && (
          <>
            <Divider sx={{ my: 1 }} />
            <ListItem disablePadding>
              <ListItemButton
                onClick={handleLogout}
                sx={{ mx: 1, borderRadius: 2, color: 'error.main' }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: 'error.main' }}>
                  <LogoutIcon />
                </ListItemIcon>
                <ListItemText primary="Logout" />
              </ListItemButton>
            </ListItem>
          </>
        )}
      </List>
    </Box>
  );

  return (
    <>
      <AppBar position="sticky" elevation={0}>
        <Toolbar sx={{ px: { xs: 2, md: 4 } }}>
          {isMobile && (
            <IconButton
              color="inherit"
              aria-label="Open navigation menu"
              edge="start"
              onClick={() => setDrawerOpen(true)}
              sx={{ mr: 1.5 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          <Box component="img" src={rbuLogo} alt="RBU Logo" sx={{ mr: 1.5, height: 36, width: 'auto', display: { xs: 'none', sm: 'block' } }} />
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{
              flexGrow: 1,
              textDecoration: 'none',
              color: 'inherit',
              fontWeight: 700,
              fontSize: { xs: '1rem', sm: '1.15rem' },
              letterSpacing: '-0.01em',
            }}
          >
            RBU Campus Exam Scheduler
          </Typography>

          {!isMobile && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {navItems.map((item) => (
                <Button
                  key={item.path}
                  component={Link}
                  to={item.path}
                  color="inherit"
                  startIcon={item.icon}
                  sx={{
                    px: 2,
                    py: 0.8,
                    borderRadius: 2,
                    backgroundColor:
                      location.pathname === item.path
                        ? 'rgba(255,255,255,0.12)'
                        : 'transparent',
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.18)',
                      transform: 'translateY(-1px)',
                    },
                  }}
                >
                  {item.label}
                </Button>
              ))}
              {isLoggedIn && (
                <Button
                  color="inherit"
                  startIcon={<LogoutIcon />}
                  onClick={handleLogout}
                  sx={{
                    px: 2,
                    py: 0.8,
                    borderRadius: 2,
                    ml: 0.5,
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.18)',
                      transform: 'translateY(-1px)',
                    },
                  }}
                >
                  Logout
                </Button>
              )}
            </Box>
          )}
        </Toolbar>
      </AppBar>

      <Drawer
        anchor="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        ModalProps={{ keepMounted: true }}
      >
        {drawer}
      </Drawer>
    </>
  );
}
