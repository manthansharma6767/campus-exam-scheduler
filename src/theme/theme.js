import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#E65100',
      light: '#FF6D00',
      dark: '#BF360C',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#FF8F00',
      light: '#FFB300',
      dark: '#E65100',
      contrastText: '#FFFFFF',
    },
    background: {
      default: '#1A1A1A',
      paper: '#242424',
    },
    text: {
      primary: '#FFFFFF',
      secondary: '#B0B0B0',
    },
    error: {
      main: '#EF5350',
    },
    success: {
      main: '#66BB6A',
    },
    divider: 'rgba(255,255,255,0.1)',
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 800,
      fontSize: '2.8rem',
      letterSpacing: '-0.02em',
    },
    h2: {
      fontWeight: 700,
      fontSize: '2rem',
      letterSpacing: '-0.01em',
    },
    h3: {
      fontWeight: 700,
      fontSize: '1.5rem',
    },
    h4: {
      fontWeight: 700,
      fontSize: '1.25rem',
    },
    h5: {
      fontWeight: 600,
      fontSize: '1.1rem',
    },
    h6: {
      fontWeight: 600,
      fontSize: '1rem',
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: '#1A1A1A',
          color: '#FFFFFF',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#2A2A2A',
          borderRadius: 16,
          border: '1px solid rgba(255,255,255,0.08)',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)',
          transition: 'box-shadow 0.3s ease, transform 0.3s ease',
          '&:hover': {
            boxShadow: '0 8px 30px rgba(230, 81, 0, 0.15)',
            transform: 'translateY(-2px)',
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          padding: '10px 24px',
          fontSize: '0.95rem',
          transition: 'all 0.2s ease',
          '&:hover': {
            transform: 'translateY(-1px)',
          },
        },
        containedPrimary: {
          background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
          boxShadow: '0 4px 14px rgba(230, 81, 0, 0.4)',
          '&:hover': {
            background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
            boxShadow: '0 6px 20px rgba(230, 81, 0, 0.5)',
          },
        },
        containedSecondary: {
          background: 'linear-gradient(135deg, #FF8F00 0%, #FFB300 100%)',
          color: '#1A1A1A',
          '&:hover': {
            background: 'linear-gradient(135deg, #E65100 0%, #FF8F00 100%)',
            color: '#FFFFFF',
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 10,
            backgroundColor: 'rgba(255,255,255,0.05)',
            '&:hover .MuiOutlinedInput-notchedOutline': {
              borderColor: '#FF8F00',
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: '#E65100',
            },
          },
          '& .MuiInputLabel-root': {
            color: '#B0B0B0',
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          background: 'rgba(26, 26, 26, 0.95)',
          backdropFilter: 'blur(20px)',
          boxShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: '#2A2A2A',
            color: '#FFFFFF',
            fontWeight: 600,
            fontSize: '0.9rem',
            borderBottom: '2px solid #E65100',
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:nth-of-type(even)': {
            backgroundColor: 'rgba(255,255,255,0.02)',
          },
          '&:hover': {
            backgroundColor: 'rgba(230, 81, 0, 0.06) !important',
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255,255,255,0.06)',
          color: '#E0E0E0',
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          backgroundColor: '#2A2A2A',
          border: '1px solid rgba(255,255,255,0.1)',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 500,
        },
        outlinedSecondary: {
          borderColor: '#FF8F00',
          color: '#FFB300',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundColor: '#242424',
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 10,
        },
      },
    },
  },
});

export default theme;
