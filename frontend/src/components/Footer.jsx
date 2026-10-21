import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import EmailIcon from '@mui/icons-material/Email';
import rbuLogo from '../assets/rbu-logo.png';

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        mt: 'auto',
        pt: 5,
        pb: 3,
        px: { xs: 2, md: 4 },
        background: 'linear-gradient(180deg, #1A1A1A 0%, #111111 100%)',
        color: 'white',
        borderTop: '1px solid rgba(255,255,255,0.06)',
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
        {/* Left: Logo + Name */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            component="img"
            src={rbuLogo}
            alt="RBU Logo"
            sx={{
              height: 48,
              width: 'auto',
              filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.3))',
            }}
          />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1.1rem', color: '#FF8F00' }}>
              RBU
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.5, mt: 0.3, fontSize: '0.8rem' }}>
              Campus Exam Scheduler
            </Typography>
          </Box>
        </Box>

        {/* Center: Quick Links */}
        <Box sx={{ textAlign: 'center' }}>
          <Typography variant="body2" sx={{ opacity: 0.4, mb: 1, fontWeight: 600, letterSpacing: '0.08em', fontSize: '0.75rem' }}>
            QUICK LINKS
          </Typography>
          <Box sx={{ display: 'flex', gap: 2.5 }}>
            {['Home', 'Search', 'Admin'].map((link) => (
              <Typography
                key={link}
                variant="body2"
                sx={{
                  opacity: 0.6,
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    opacity: 1,
                    color: '#FF8F00',
                  },
                }}
              >
                {link}
              </Typography>
            ))}
          </Box>
        </Box>

        {/* Right: Copyright + Social */}
        <Box sx={{ textAlign: { xs: 'center', md: 'right' } }}>
          <Box sx={{ display: 'flex', gap: 0.5, justifyContent: { xs: 'center', md: 'flex-end' }, mb: 1 }}>
            {[
              { icon: <GitHubIcon fontSize="small" />, label: 'GitHub' },
              { icon: <LinkedInIcon fontSize="small" />, label: 'LinkedIn' },
              { icon: <EmailIcon fontSize="small" />, label: 'Email' },
            ].map((social) => (
              <IconButton
                key={social.label}
                size="small"
                aria-label={social.label}
                sx={{
                  color: 'rgba(255,255,255,0.3)',
                  '&:hover': {
                    color: '#FF8F00',
                    backgroundColor: 'rgba(230, 81, 0, 0.08)',
                  },
                }}
              >
                {social.icon}
              </IconButton>
            ))}
          </Box>
          <Typography variant="body2" sx={{ opacity: 0.4, fontSize: '0.8rem' }}>
            &copy; {new Date().getFullYear()} RBU, Nagpur
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.25, display: 'block', mt: 0.3 }}>
            All rights reserved
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
