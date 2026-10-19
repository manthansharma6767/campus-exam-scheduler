import { useState, useEffect } from 'react';
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
import Tooltip from '@mui/material/Tooltip';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Snackbar from '@mui/material/Snackbar';
import Alert from '@mui/material/Alert';
import useMediaQuery from '@mui/material/useMediaQuery';
import { useTheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import HomeIcon from '@mui/icons-material/Home';
import SearchIcon from '@mui/icons-material/Search';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import HistoryIcon from '@mui/icons-material/History';
import EmailIcon from '@mui/icons-material/Email';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LogoutIcon from '@mui/icons-material/Logout';
import DeleteIcon from '@mui/icons-material/Delete';
import rbuLogo from '../assets/rbu-logo.png';

export default function Navbar() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [historyAnchor, setHistoryAnchor] = useState(null);
  const [recentSearches, setRecentSearches] = useState([]);
  const [snackbar, setSnackbar] = useState({ open: false, message: '' });

  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isLoggedIn = !!localStorage.getItem('token');

  // Load history from localStorage
  useEffect(() => {
    try {
      const history = JSON.parse(localStorage.getItem('search_history') || '[]');
      setRecentSearches(history);
    } catch {
      setRecentSearches([]);
    }
  }, [historyAnchor, location.pathname]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/admin/login');
    setDrawerOpen(false);
  };

  const handleHomeClick = () => {
    if (location.pathname !== '/') {
      navigate('/');
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleSearchClick = () => {
    if (location.pathname !== '/') {
      navigate('/');
      setTimeout(() => {
        const searchInput = document.getElementById('enrollment-search-input');
        if (searchInput) {
          searchInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
          searchInput.focus();
        }
      }, 300);
    } else {
      const searchInput = document.getElementById('enrollment-search-input');
      if (searchInput) {
        searchInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        searchInput.focus();
      }
    }
  };

  const handleHistoryClick = (e) => {
    setHistoryAnchor(e.currentTarget);
  };

  const handleHistoryClose = () => {
    setHistoryAnchor(null);
  };

  const handleSelectHistory = (enrollment) => {
    handleHistoryClose();
    navigate(`/search/${encodeURIComponent(enrollment)}`);
  };

  const handleClearHistory = () => {
    localStorage.removeItem('search_history');
    setRecentSearches([]);
    handleHistoryClose();
  };

  const adminItems = isLoggedIn
    ? [{ label: 'Dashboard', path: '/admin/dashboard', icon: <DashboardIcon /> }]
    : [{ label: 'Admin', path: '/admin/login', icon: <AdminPanelSettingsIcon /> }];

  const drawer = (
    <Box sx={{ width: 280 }} role="navigation" aria-label="Main navigation">
      <Box
        sx={{
          p: 2.5,
          background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 100%)',
          color: 'white',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box
            component="img"
            src={rbuLogo}
            alt="RBU Logo"
            sx={{ height: 40, width: 'auto' }}
          />
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
      <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)' }} />
      <List sx={{ pt: 1 }}>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => {
              setDrawerOpen(false);
              handleHomeClick();
            }}
            selected={location.pathname === '/'}
            sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: location.pathname === '/' ? '#FF8F00' : '#B0B0B0' }}>
              <HomeIcon />
            </ListItemIcon>
            <ListItemText primary="Home" />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => {
              setDrawerOpen(false);
              handleSearchClick();
            }}
            sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: '#B0B0B0' }}>
              <SearchIcon />
            </ListItemIcon>
            <ListItemText primary="Search Exams" />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton
            onClick={(e) => {
              setDrawerOpen(false);
              handleHistoryClick(e);
            }}
            sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: '#B0B0B0' }}>
              <HistoryIcon />
            </ListItemIcon>
            <ListItemText primary="Search History" />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => {
              setDrawerOpen(false);
              navigate('/calendar');
            }}
            sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: '#B0B0B0' }}>
              <CalendarMonthIcon />
            </ListItemIcon>
            <ListItemText primary="My Schedule" />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => {
              setDrawerOpen(false);
              setSnackbar({ open: true, message: 'Contact: Please reach out to your exam administration office.' });
            }}
            sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40, color: '#B0B0B0' }}>
              <EmailIcon />
            </ListItemIcon>
            <ListItemText primary="Contact" />
          </ListItemButton>
        </ListItem>
        <Divider sx={{ my: 1, borderColor: 'rgba(255,255,255,0.08)' }} />
        {adminItems.map((item) => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton
              component={Link}
              to={item.path}
              selected={location.pathname === item.path}
              onClick={() => setDrawerOpen(false)}
              sx={{ mx: 1, borderRadius: 2, mb: 0.5 }}
            >
              <ListItemIcon sx={{ minWidth: 40, color: location.pathname === item.path ? '#FF8F00' : '#B0B0B0' }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          </ListItem>
        ))}
        {isLoggedIn && (
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
        )}
      </List>
    </Box>
  );

  return (
    <>
      <AppBar position="sticky" elevation={0}>
        <Toolbar sx={{ px: { xs: 2, md: 4 }, minHeight: { xs: 64, md: 70 } }}>
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

          {/* Logo */}
          <Box
            component={Link}
            to="/"
            sx={{
              display: 'flex',
              alignItems: 'center',
              textDecoration: 'none',
              color: 'inherit',
              mr: 2,
            }}
          >
            <Box
              component="img"
              src={rbuLogo}
              alt="RBU Logo"
              sx={{
                height: { xs: 36, md: 44 },
                width: 'auto',
                filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.3))',
              }}
            />
          </Box>

          {/* Desktop: Icon Navigation - centered */}
          {!isMobile && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: { md: 2, lg: 4 },
                flexGrow: 1,
                justifyContent: 'center',
              }}
            >
              <Tooltip title="Home" arrow>
                <IconButton
                  onClick={handleHomeClick}
                  sx={{
                    color: location.pathname === '/' ? '#FFFFFF' : 'rgba(255,255,255,0.6)',
                    '&:hover': { color: '#FFFFFF', backgroundColor: 'rgba(255,255,255,0.08)' },
                  }}
                  aria-label="Home"
                >
                  <HomeIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="Search" arrow>
                <IconButton
                  onClick={handleSearchClick}
                  sx={{
                    color: 'rgba(255,255,255,0.6)',
                    '&:hover': { color: '#FFFFFF', backgroundColor: 'rgba(255,255,255,0.08)' },
                  }}
                  aria-label="Search"
                >
                  <SearchIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="My Schedule" arrow>
                <IconButton
                  onClick={() => navigate('/calendar')}
                  sx={{
                    color: 'rgba(255,255,255,0.6)',
                    '&:hover': { color: '#FFFFFF', backgroundColor: 'rgba(255,255,255,0.08)' },
                  }}
                  aria-label="Calendar"
                >
                  <CalendarMonthIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="Recent Search History" arrow>
                <IconButton
                  onClick={handleHistoryClick}
                  sx={{
                    color: historyAnchor ? '#FF8F00' : 'rgba(255,255,255,0.6)',
                    '&:hover': { color: '#FFFFFF', backgroundColor: 'rgba(255,255,255,0.08)' },
                  }}
                  aria-label="Search History"
                >
                  <HistoryIcon />
                </IconButton>
              </Tooltip>

              <Tooltip title="Contact" arrow>
                <IconButton
                  onClick={() => setSnackbar({ open: true, message: 'Contact: Please reach out to your exam administration office.' })}
                  sx={{
                    color: 'rgba(255,255,255,0.6)',
                    '&:hover': { color: '#FFFFFF', backgroundColor: 'rgba(255,255,255,0.08)' },
                  }}
                  aria-label="Contact"
                >
                  <EmailIcon />
                </IconButton>
              </Tooltip>
            </Box>
          )}

          {/* Spacer for mobile */}
          {isMobile && <Box sx={{ flexGrow: 1 }} />}

          {/* Public exam search + Admin actions */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {!isMobile && (
              <Button
                onClick={handleSearchClick}
                variant="contained"
                sx={{
                  background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                  color: '#FFFFFF',
                  fontWeight: 700,
                  fontSize: '0.8rem',
                  letterSpacing: '0.05em',
                  px: 2.5,
                  py: 0.8,
                  borderRadius: 2,
                  boxShadow: '0 2px 10px rgba(230, 81, 0, 0.35)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                    boxShadow: '0 4px 16px rgba(230, 81, 0, 0.5)',
                    transform: 'translateY(-1px)',
                  },
                }}
              >
                FIND MY EXAMS
              </Button>
            )}
            {isLoggedIn && !isMobile && (
              <Tooltip title="Logout" arrow>
                <IconButton
                  onClick={handleLogout}
                  sx={{
                    color: 'rgba(255,255,255,0.6)',
                    '&:hover': {
                      color: '#EF5350',
                      backgroundColor: 'rgba(239, 83, 80, 0.08)',
                    },
                  }}
                  aria-label="Logout"
                >
                  <LogoutIcon />
                </IconButton>
              </Tooltip>
            )}
          </Box>
        </Toolbar>
      </AppBar>

      {/* History Menu */}
      <Menu
        anchorEl={historyAnchor}
        open={Boolean(historyAnchor)}
        onClose={handleHistoryClose}
        PaperProps={{
          sx: {
            backgroundColor: '#242424',
            color: '#FFFFFF',
            border: '1px solid rgba(255,255,255,0.1)',
            minWidth: 220,
            mt: 1,
          },
        }}
      >
        <Box sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#FF8F00' }}>
            Recent Searches
          </Typography>
          {recentSearches.length > 0 && (
            <IconButton size="small" onClick={handleClearHistory} sx={{ color: 'rgba(255,255,255,0.5)', '&:hover': { color: '#EF5350' } }}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)' }} />
        {recentSearches.length === 0 ? (
          <MenuItem disabled sx={{ color: 'rgba(255,255,255,0.4)', fontSize: '0.85rem' }}>
            No recent searches
          </MenuItem>
        ) : (
          recentSearches.map((item, idx) => (
            <MenuItem
              key={idx}
              onClick={() => handleSelectHistory(item)}
              sx={{
                fontSize: '0.9rem',
                '&:hover': { backgroundColor: 'rgba(230, 81, 0, 0.15)', color: '#FFB300' },
              }}
            >
              {item}
            </MenuItem>
          ))
        )}
      </Menu>

      {/* Coming Soon Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity="info"
          variant="filled"
          sx={{ backgroundColor: '#E65100', color: '#FFFFFF', fontWeight: 600 }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>

      <Drawer
        anchor="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        ModalProps={{ keepMounted: true }}
        PaperProps={{
          sx: {
            backgroundColor: '#1A1A1A',
            color: '#FFFFFF',
          },
        }}
      >
        {drawer}
      </Drawer>
    </>
  );
}
