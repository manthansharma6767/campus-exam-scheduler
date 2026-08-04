import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';
import Fade from '@mui/material/Fade';
import SearchBox from '../components/SearchBox';
import rbuLogo from '../assets/rbu-logo.png';

export default function Home() {
  return (
    <Fade in timeout={600}>
      <Box>
        {/* Hero Section */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #0A1929 0%, #1A3A5C 50%, #0D47A1 100%)',
            color: 'white',
            py: { xs: 8, md: 12 },
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          {/* Subtle background pattern */}
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              opacity: 0.05,
              backgroundImage:
                'radial-gradient(circle at 25% 25%, white 1px, transparent 1px), radial-gradient(circle at 75% 75%, white 1px, transparent 1px)',
              backgroundSize: '50px 50px',
            }}
          />

          <Container maxWidth="md" sx={{ position: 'relative', zIndex: 1 }}>
            <Box sx={{ textAlign: 'center' }}>
              <Box
                sx={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 1,
                  backgroundColor: 'rgba(255,255,255,0.1)',
                  backdropFilter: 'blur(10px)',
                  borderRadius: 3,
                  px: 2.5,
                  py: 1,
                  mb: 3,
                }}
              >
                <Box component="img" src={rbuLogo} alt="RBU Logo" sx={{ height: 24, width: 'auto' }} />
                <Typography variant="body2" sx={{ fontWeight: 600, letterSpacing: '0.05em' }}>
                  RBU UNIVERSITY
                </Typography>
              </Box>

              <Typography
                variant="h1"
                sx={{
                  fontSize: { xs: '2rem', sm: '2.5rem', md: '3.2rem' },
                  fontWeight: 800,
                  lineHeight: 1.15,
                  mb: 2,
                  letterSpacing: '-0.03em',
                }}
              >
                Campus Exam
                <br />
                <Box
                  component="span"
                  sx={{
                    background: 'linear-gradient(90deg, #42A5F5, #90CAF9)',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                  }}
                >
                  Scheduler
                </Box>
              </Typography>

              <Typography
                variant="h6"
                sx={{
                  opacity: 0.8,
                  fontWeight: 400,
                  maxWidth: 520,
                  mx: 'auto',
                  mb: 5,
                  lineHeight: 1.6,
                  fontSize: { xs: '0.95rem', md: '1.1rem' },
                }}
              >
                Look up your exam schedule instantly. Enter your enrollment number below
                to view upcoming exams, timings, and room assignments.
              </Typography>

              {/* Search Box */}
              <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                <SearchBox />
              </Box>
            </Box>
          </Container>
        </Box>

        {/* Features Section */}
        <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' },
              gap: 3,
            }}
          >
            {[
              {
                icon: '🔍',
                title: 'Quick Search',
                description: 'Find your exam schedule in seconds using your enrollment number.',
              },
              {
                icon: '📋',
                title: 'Complete Details',
                description: 'View subject, date, time, building, and room number at a glance.',
              },
              {
                icon: '📱',
                title: 'Access Anywhere',
                description: 'Works seamlessly on desktop, tablet, and mobile devices.',
              },
            ].map((feature, index) => (
              <Box
                key={index}
                sx={{
                  textAlign: 'center',
                  p: 4,
                  borderRadius: 3,
                  backgroundColor: '#F5F7FA',
                  border: '1px solid #E2E8F0',
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    boxShadow: '0 8px 25px rgba(10, 25, 41, 0.1)',
                    transform: 'translateY(-2px)',
                  },
                }}
              >
                <Typography sx={{ fontSize: '2.5rem', mb: 1.5 }}>{feature.icon}</Typography>
                <Typography variant="h5" sx={{ mb: 1, fontWeight: 700 }}>
                  {feature.title}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                  {feature.description}
                </Typography>
              </Box>
            ))}
          </Box>
        </Container>
      </Box>
    </Fade>
  );
}
