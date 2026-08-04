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
      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Loading message="Loading exam schedule..." />
      </Container>
    );
  }

  return (
    <Fade in timeout={600}>
      <Box>
        {/* Header */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #0A1929 0%, #1A3A5C 100%)',
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
                color: 'rgba(255,255,255,0.8)',
                mb: 2,
                '&:hover': {
                  color: 'white',
                  backgroundColor: 'rgba(255,255,255,0.1)',
                },
              }}
            >
              Back to Search
            </Button>
            <Typography
              variant="h3"
              sx={{
                fontWeight: 700,
                fontSize: { xs: '1.5rem', md: '2rem' },
              }}
            >
              Exam Schedule
            </Typography>
            <Typography
              variant="body1"
              sx={{ opacity: 0.8, mt: 0.5 }}
            >
              Enrollment: <strong>{enrollmentNumber}</strong>
            </Typography>
          </Container>
        </Box>

        <Container maxWidth="lg" sx={{ py: { xs: 4, md: 5 } }}>
          {/* Student Not Found */}
          {error === 'student_not_found' && (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <PersonOffIcon sx={{ fontSize: 72, color: 'text.secondary', opacity: 0.4, mb: 2 }} />
              <Typography variant="h5" sx={{ fontWeight: 600, mb: 1 }}>
                Student Not Found
              </Typography>
              <Typography color="text.secondary" sx={{ mb: 3, maxWidth: 420, mx: 'auto' }}>
                No student found with enrollment number <strong>{enrollmentNumber}</strong>.
                Please check the number and try again.
              </Typography>
              <Button
                component={Link}
                to="/"
                variant="contained"
                color="secondary"
                startIcon={<ArrowBackIcon />}
              >
                Search Again
              </Button>
            </Box>
          )}

          {/* Unexpected Error */}
          {error === 'unexpected' && (
            <Alert severity="error" sx={{ borderRadius: 3 }}>
              An unexpected error occurred. Please try again later.
            </Alert>
          )}

          {/* No Exams Scheduled */}
          {!error && exams.length === 0 && (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <EventBusyIcon sx={{ fontSize: 72, color: 'text.secondary', opacity: 0.4, mb: 2 }} />
              <Typography variant="h5" sx={{ fontWeight: 600, mb: 1 }}>
                No Exams Scheduled
              </Typography>
              <Typography color="text.secondary" sx={{ mb: 3, maxWidth: 420, mx: 'auto' }}>
                There are currently no exams scheduled for enrollment number{' '}
                <strong>{enrollmentNumber}</strong>.
              </Typography>
              <Button
                component={Link}
                to="/"
                variant="contained"
                color="secondary"
                startIcon={<ArrowBackIcon />}
              >
                Search Again
              </Button>
            </Box>
          )}

          {/* Exam Results */}
          {!error && exams.length > 0 && (
            <>
              <Typography variant="h5" sx={{ fontWeight: 600, mb: 3 }}>
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
    </Fade>
  );
}
