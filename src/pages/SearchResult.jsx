import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Fade from '@mui/material/Fade';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import ExamCard from '../components/ExamCard';
import Loading from '../components/Loading';
import api from '../services/api';

export default function SearchResult() {
  const { enrollmentNumber } = useParams();
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSchedule = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await api.get(
          `/api/students/${encodeURIComponent(enrollmentNumber)}/schedule`
        );
        setExams(response.data);
      } catch (err) {
        if (err.response && err.response.status === 404) {
          setError('student_not_found');
        } else {
          setError('unexpected');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchSchedule();
  }, [enrollmentNumber]);

  if (loading) {
    return (
      <Box
        sx={{
          minHeight: '60vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'linear-gradient(180deg, #2D1500 0%, #1A1A1A 100%)',
        }}
      >
        <Loading message="Loading exam schedule..." />
      </Box>
    );
  }

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
            <Button
              component={Link}
              to="/"
              startIcon={<ArrowBackIcon />}
              sx={{
                color: 'rgba(255,255,255,0.7)',
                mb: 2,
                '&:hover': {
                  color: '#FF8F00',
                  backgroundColor: 'rgba(230, 81, 0, 0.08)',
                },
              }}
            >
              Back to Search
            </Button>
            <Typography
              variant="h3"
              sx={{
                fontWeight: 800,
                fontSize: { xs: '1.5rem', md: '2.2rem' },
                fontFamily: '"Playfair Display", "Georgia", serif',
                fontStyle: 'italic',
              }}
            >
              Exam Schedule
            </Typography>
            <Typography
              variant="body1"
              sx={{ opacity: 0.7, mt: 0.5 }}
            >
              Enrollment: <Box component="strong" sx={{ color: '#FF8F00' }}>{enrollmentNumber}</Box>
            </Typography>
          </Container>
        </Box>

        <Box
          sx={{
            background: '#1A1A1A',
            minHeight: '50vh',
          }}
        >
          <Container maxWidth="lg" sx={{ py: { xs: 4, md: 5 } }}>
            {/* Student Not Found */}
            {error === 'student_not_found' && (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <PersonOffIcon sx={{ fontSize: 72, color: '#B0B0B0', opacity: 0.3, mb: 2 }} />
                <Typography variant="h5" sx={{ fontWeight: 600, mb: 1, color: '#FFFFFF' }}>
                  Student Not Found
                </Typography>
                <Typography sx={{ mb: 3, maxWidth: 420, mx: 'auto', color: '#B0B0B0' }}>
                  No student found with enrollment number <strong>{enrollmentNumber}</strong>.
                  Please check the number and try again.
                </Typography>
                <Button
                  component={Link}
                  to="/"
                  variant="contained"
                  startIcon={<ArrowBackIcon />}
                  sx={{
                    background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                    '&:hover': {
                      background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                    },
                  }}
                >
                  Search Again
                </Button>
              </Box>
            )}

            {/* Unexpected Error */}
            {error === 'unexpected' && (
              <Alert severity="error" sx={{ borderRadius: 3, backgroundColor: 'rgba(239, 83, 80, 0.1)', color: '#EF5350' }}>
                An unexpected error occurred. Please try again later.
              </Alert>
            )}

            {/* No Exams Scheduled */}
            {!error && exams.length === 0 && (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <EventBusyIcon sx={{ fontSize: 72, color: '#B0B0B0', opacity: 0.3, mb: 2 }} />
                <Typography variant="h5" sx={{ fontWeight: 600, mb: 1, color: '#FFFFFF' }}>
                  No Exams Scheduled
                </Typography>
                <Typography sx={{ mb: 3, maxWidth: 420, mx: 'auto', color: '#B0B0B0' }}>
                  There are currently no exams scheduled for enrollment number{' '}
                  <strong>{enrollmentNumber}</strong>.
                </Typography>
                <Button
                  component={Link}
                  to="/"
                  variant="contained"
                  startIcon={<ArrowBackIcon />}
                  sx={{
                    background: 'linear-gradient(135deg, #E65100 0%, #FF6D00 100%)',
                    '&:hover': {
                      background: 'linear-gradient(135deg, #BF360C 0%, #E65100 100%)',
                    },
                  }}
                >
                  Search Again
                </Button>
              </Box>
            )}

            {/* Exam Results */}
            {!error && exams.length > 0 && (
              <>
                <Typography variant="h5" sx={{ fontWeight: 600, mb: 3, color: '#FFB300' }}>
                  {exams.length} Exam{exams.length !== 1 ? 's' : ''} Found
                </Typography>
                <Grid container spacing={3}>
                  {exams.map((exam, index) => (
                    <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={index}>
                      <ExamCard exam={exam} />
                    </Grid>
                  ))}
                </Grid>
              </>
            )}
          </Container>
        </Box>
      </Box>
    </Fade>
  );
}
