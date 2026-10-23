import { useRef } from 'react';
import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import Fade from '@mui/material/Fade';
import SearchBox from '../components/SearchBox';
import ChairIcon from '@mui/icons-material/Chair';
import handPencil from '../assets/hand-pencil.png';

export default function Home() {
  const searchRef = useRef(null);

  const scrollToSearch = () => {
    searchRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <Fade in timeout={600}>
      <Box>
        {/* Hero Section — matching McGill Exam Scheduler design */}
        <Box
          sx={{
            background: 'linear-gradient(160deg, #1A1A1A 0%, #2D1500 35%, #4A1800 60%, #6B2000 85%, #8B2500 100%)',
            color: 'white',
            minHeight: { xs: 'calc(100vh - 70px)', md: '85vh' },
            display: 'flex',
            alignItems: 'center',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          {/* Subtle overlay gradient for depth */}
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'radial-gradient(ellipse at 80% 50%, rgba(139, 37, 0, 0.4) 0%, transparent 60%)',
              pointerEvents: 'none',
            }}
          />

          {/* Animated subtle particles/dots */}
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              opacity: 0.03,
              backgroundImage:
                'radial-gradient(circle at 20% 30%, white 1px, transparent 1px), radial-gradient(circle at 80% 70%, white 1px, transparent 1px)',
              backgroundSize: '60px 60px',
              pointerEvents: 'none',
            }}
          />

          <Container maxWidth="xl" sx={{ position: 'relative', zIndex: 1 }}>
            <Box
              sx={{
                display: 'flex',
                flexDirection: { xs: 'column', md: 'row' },
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: { xs: 4, md: 0 },
                py: { xs: 4, md: 0 },
              }}
            >
              {/* Left Content */}
              <Box sx={{ maxWidth: { xs: '100%', md: '55%' }, px: { xs: 0, md: 4 } }}>
                <Fade in timeout={800}>
                  <Typography
                    variant="h1"
                    sx={{
                      fontSize: { xs: '2.2rem', sm: '2.8rem', md: '3.5rem', lg: '4rem' },
                      fontWeight: 800,
                      lineHeight: 1.1,
                      mb: 3,
                      fontFamily: '"Playfair Display", "Georgia", serif',
                      fontStyle: 'italic',
                      textShadow: '0 2px 10px rgba(0,0,0,0.3)',
                    }}
                  >
                    Welcome To
                    <br />
                    <Box component="span" sx={{ color: '#FF8F00' }}>
                      RBU
                    </Box>
                    {' '}Exam Scheduler
                    <Box
                      component="span"
                      sx={{
                        color: '#FF8F00',
                        display: 'inline-block',
                        ml: 0.5,
                      }}
                    >
                      !
                    </Box>
                  </Typography>
                </Fade>

                <Fade in timeout={1000}>
                  <Box>
                    <Typography
                      variant="body1"
                      sx={{
                        opacity: 0.85,
                        fontSize: { xs: '0.95rem', md: '1.05rem' },
                        lineHeight: 1.7,
                        mb: 0.5,
                        maxWidth: 480,
                      }}
                    >
                      Add your exams to your calendar with ease!
                    </Typography>
                    <Typography
                      variant="body1"
                      sx={{
                        opacity: 0.6,
                        fontSize: { xs: '0.9rem', md: '1rem' },
                        lineHeight: 1.7,
                        mb: 4,
                        fontStyle: 'italic',
                        maxWidth: 480,
                      }}
                    >
                      Just search, click add, and export...
                    </Typography>
                  </Box>
                </Fade>

                <Fade in timeout={1200}>
                  <Button
                    variant="contained"
                    size="large"
                    onClick={scrollToSearch}
                    sx={{
                      background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                      color: '#FFFFFF',
                      fontWeight: 700,
                      fontSize: { xs: '0.95rem', md: '1.05rem' },
                      letterSpacing: '0.1em',
                      px: { xs: 4, md: 5 },
                      py: { xs: 1.5, md: 1.8 },
                      borderRadius: 2,
                      boxShadow: '0 4px 20px rgba(230, 81, 0, 0.45)',
                      textTransform: 'uppercase',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                        boxShadow: '0 6px 28px rgba(230, 81, 0, 0.6)',
                        transform: 'translateY(-2px)',
                      },
                      transition: 'all 0.3s ease',
                    }}
                  >
                    GET STARTED
                  </Button>
                </Fade>
              </Box>

              {/* Right Content — Pencil Illustration */}
              <Box
                sx={{
                  display: { xs: 'none', md: 'flex' },
                  alignItems: 'center',
                  justifyContent: 'center',
                  maxWidth: '45%',
                }}
              >
                <Fade in timeout={1400}>
                  <Box
                    component="img"
                    src={handPencil}
                    alt="Writing hand illustration"
                    sx={{
                      width: '100%',
                      maxWidth: 420,
                      height: 'auto',
                      filter: 'drop-shadow(0 10px 30px rgba(0,0,0,0.4))',
                      animation: 'float 4s ease-in-out infinite',
                      '@keyframes float': {
                        '0%, 100%': { transform: 'translateY(0px)' },
                        '50%': { transform: 'translateY(-12px)' },
                      },
                    }}
                  />
                </Fade>
              </Box>
            </Box>
          </Container>
        </Box>

        {/* Search Section */}
        <Box
          ref={searchRef}
          sx={{
            background: 'linear-gradient(180deg, #2D1500 0%, #1A1A1A 100%)',
            py: { xs: 6, md: 8 },
          }}
        >
          <Container maxWidth="md">
            <Fade in timeout={600}>
              <Box>
                <Typography
                  variant="h2"
                  sx={{
                    fontWeight: 800,
                    fontSize: { xs: '1.8rem', md: '2.5rem' },
                    mb: 3,
                    fontFamily: '"Playfair Display", "Georgia", serif',
                    fontStyle: 'italic',
                  }}
                >
                  Search
                </Typography>

                <Typography
                  variant="body1"
                  sx={{
                    opacity: 0.85,
                    lineHeight: 1.8,
                    mb: 2,
                    maxWidth: 600,
                  }}
                >
                  Find your exam schedule by entering your enrollment number (e.g. RBU2024001). 
                  Feel free to search to find your personalized exam schedule!
                </Typography>

                <Typography
                  variant="body2"
                  sx={{
                    opacity: 0.65,
                    lineHeight: 1.8,
                    mb: 4,
                    maxWidth: 600,
                  }}
                >
                  Enter your enrollment number to see your upcoming exams, including dates, times, and room assignments.
                </Typography>

                {/* Search Box */}
                <Box sx={{ mb: 4 }}>
                  <SearchBox />
                </Box>
                <Button component={Link} to="/seating" variant="outlined" startIcon={<ChairIcon />} sx={{ borderColor: 'rgba(255,143,0,0.5)', color: '#FFB300' }}>
                  Find my seat by roll number
                </Button>
              </Box>
            </Fade>
          </Container>
        </Box>

        {/* Features Section */}
        <Box
          sx={{
            background: '#1A1A1A',
            py: { xs: 6, md: 8 },
          }}
        >
          <Container maxWidth="lg">
            <Typography
              variant="h3"
              sx={{
                textAlign: 'center',
                fontWeight: 700,
                mb: 5,
                color: '#FF8F00',
              }}
            >
              Why Use RBU Exam Scheduler?
            </Typography>
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
                <Fade in timeout={800 + index * 200} key={index}>
                  <Box
                    sx={{
                      textAlign: 'center',
                      p: 4,
                      borderRadius: 3,
                      backgroundColor: 'rgba(255,255,255,0.03)',
                      border: '1px solid rgba(255,255,255,0.06)',
                      transition: 'all 0.3s ease',
                      '&:hover': {
                        backgroundColor: 'rgba(230, 81, 0, 0.06)',
                        borderColor: 'rgba(230, 81, 0, 0.2)',
                        boxShadow: '0 8px 30px rgba(230, 81, 0, 0.1)',
                        transform: 'translateY(-4px)',
                      },
                    }}
                  >
                    <Typography sx={{ fontSize: '2.5rem', mb: 1.5 }}>{feature.icon}</Typography>
                    <Typography variant="h5" sx={{ mb: 1, fontWeight: 700, color: '#FFB300' }}>
                      {feature.title}
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#B0B0B0', lineHeight: 1.7 }}>
                      {feature.description}
                    </Typography>
                  </Box>
                </Fade>
              ))}
            </Box>
          </Container>
        </Box>
      </Box>
    </Fade>
  );
}
