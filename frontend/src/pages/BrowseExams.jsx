import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Fade from '@mui/material/Fade';
import Grid from '@mui/material/Grid';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import SearchIcon from '@mui/icons-material/Search';
import ExamCard from '../components/ExamCard';
import Loading from '../components/Loading';
import api from '../services/api';
import { getExamKey, readPersonalSchedule, saveExamToPersonalSchedule, removeExamFromPersonalSchedule } from '../utils/personalSchedule';

const emptyFilters = { query: '', semester: '', date: '' };

function savedBrowseKeys() {
  return new Set(readPersonalSchedule()
    .filter((exam) => !exam.enrollmentNumber)
    .map((exam) => getExamKey(exam, '')));
}

export default function BrowseExams() {
  const [filters, setFilters] = useState(emptyFilters);
  const [submittedFilters, setSubmittedFilters] = useState(emptyFilters);
  const [exams, setExams] = useState([]);
  const [selectedKeys, setSelectedKeys] = useState(savedBrowseKeys);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchExams = async () => {
      setLoading(true);
      setError('');
      try {
        const params = Object.fromEntries(
          Object.entries(submittedFilters).filter(([, value]) => value !== '')
        );
        const response = await api.get('/api/exams/search', { params });
        setExams(response.data);
      } catch {
        setError('Could not load exams. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    fetchExams();
  }, [submittedFilters]);

  const handleFilterChange = (event) => {
    setFilters({ ...filters, [event.target.name]: event.target.value });
  };

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedFilters({ ...filters, query: filters.query.trim() });
  };

  const clearFilters = () => {
    setFilters(emptyFilters);
    setSubmittedFilters({ ...emptyFilters });
  };

  const toggleExam = (exam) => {
    const key = getExamKey(exam, '');
    if (selectedKeys.has(key)) {
      removeExamFromPersonalSchedule(exam, '');
      const next = new Set(selectedKeys);
      next.delete(key);
      setSelectedKeys(next);
    } else {
      saveExamToPersonalSchedule(exam, '');
      setSelectedKeys(new Set([...selectedKeys, key]));
    }
  };

  const addAll = () => {
    exams.forEach((exam) => saveExamToPersonalSchedule(exam, ''));
    setSelectedKeys(savedBrowseKeys());
  };

  const clearResults = () => {
    exams.forEach((exam) => removeExamFromPersonalSchedule(exam, ''));
    setSelectedKeys(savedBrowseKeys());
  };

  return (
    <Fade in timeout={600}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)', color: 'white', py: { xs: 4, md: 5 } }}>
          <Container maxWidth="lg">
            <Button component={Link} to="/" startIcon={<ArrowBackIcon />} sx={{ color: 'rgba(255,255,255,0.7)', mb: 2 }}>
              Back to Home
            </Button>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
              <SearchIcon sx={{ color: '#FF8F00', fontSize: 32 }} />
              <Typography variant="h3" sx={{ fontWeight: 800, fontSize: { xs: '1.5rem', md: '2.2rem' } }}>
                Browse Exam Schedules
              </Typography>
            </Stack>
            <Typography sx={{ opacity: 0.75, mt: 1 }}>
              Find exams by subject, course code, venue, semester, or date. Save the exams you want to your personal calendar.
            </Typography>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '55vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 3, md: 4 } }}>
            <Paper component="form" onSubmit={handleSearch} elevation={0} sx={{ p: { xs: 2, md: 3 }, mb: 3, border: '1px solid rgba(255,255,255,0.08)', borderRadius: 3, backgroundColor: '#242424' }}>
              <Grid container spacing={2} sx={{ alignItems: 'center' }}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField
                    name="query"
                    label="Subject, course code, building, or room"
                    value={filters.query}
                    onChange={handleFilterChange}
                    fullWidth
                    slotProps={{ htmlInput: { 'aria-label': 'Search exams by subject, course code, building or room' } }}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <TextField name="semester" label="Semester" select value={filters.semester} onChange={handleFilterChange} fullWidth>
                    <MenuItem value="">Any semester</MenuItem>
                    {[1, 2, 3, 4, 5, 6, 7, 8].map((semester) => (
                      <MenuItem key={semester} value={semester}>Semester {semester}</MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <TextField name="date" label="Exam date" type="date" value={filters.date} onChange={handleFilterChange} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={12}>
                  <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end' }}>
                    <Button type="button" variant="outlined" onClick={clearFilters}>Clear filters</Button>
                    <Button type="submit" variant="contained" startIcon={<SearchIcon />}>Search Exams</Button>
                  </Stack>
                </Grid>
              </Grid>
            </Paper>

            {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
            {loading ? (
              <Loading message="Loading exam schedules..." />
            ) : exams.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 7 }}>
                <Typography variant="h5" sx={{ color: '#FFFFFF', mb: 1 }}>No exams found</Typography>
                <Typography sx={{ color: '#B0B0B0' }}>Try changing your search or clearing the filters.</Typography>
              </Box>
            ) : (
              <>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3, justifyContent: 'space-between', alignItems: { xs: 'stretch', sm: 'center' } }}>
                  <Typography variant="h5" sx={{ fontWeight: 600, color: '#FFB300' }}>
                    {exams.length} exam{exams.length === 1 ? '' : 's'} found · {selectedKeys.size} saved from browse
                  </Typography>
                  <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <Button size="small" onClick={addAll}>Add results</Button>
                    <Button size="small" onClick={clearResults}>Clear results</Button>
                    <Button component={Link} to="/calendar" size="small" startIcon={<CalendarMonthIcon />}>My Schedule</Button>
                  </Stack>
                </Stack>
                <Grid container spacing={3}>
                  {exams.map((exam) => {
                    const key = getExamKey(exam, '');
                    return (
                      <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={key}>
                        <ExamCard exam={exam} selected={selectedKeys.has(key)} onToggle={() => toggleExam(exam)} />
                      </Grid>
                    );
                  })}
                </Grid>
              </>
            )}
          </Container>
        </Box>
      </Box>
    </Fade>
  );
}
