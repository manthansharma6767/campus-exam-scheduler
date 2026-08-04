import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import rbuLogo from '../assets/rbu-logo.png';

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        mt: 'auto',
        pt: 4,
        pb: 3,
        px: { xs: 2, md: 4 },
        backgroundColor: '#0A1929',
        color: 'white',
      }}
    >
      <Box
        sx={{
          maxWidth: 1200,
          mx: 'auto',
          display: 'flex',
          flexDirection: { xs: 'column', md: 'row' },
          alignItems: { xs: 'center', md: 'flex-start' },
          justifyContent: 'space-between',
          gap: 3,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Box component="img" src={rbuLogo} alt="RBU Logo" sx={{ height: 36, width: 'auto' }} />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1.05rem' }}>
              RBU Campus Exam Scheduler
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.3 }}>
              Streamlining examination management
            </Typography>
          </Box>
        </Box>

        <Box sx={{ textAlign: { xs: 'center', md: 'right' } }}>
          <Typography variant="body2" sx={{ opacity: 0.6 }}>
            &copy; {new Date().getFullYear()} RBU University. All rights reserved.
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.4, display: 'block', mt: 0.5 }}>
            Campus Exam Scheduling System
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
