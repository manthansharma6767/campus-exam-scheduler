import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Alert from '@mui/material/Alert';
import Fade from '@mui/material/Fade';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import DownloadIcon from '@mui/icons-material/Download';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import ExamCard from '../components/ExamCard';
import { downloadCalendar, readPersonalSchedule, removeExamFromPersonalSchedule } from '../utils/personalSchedule';

export default function MySchedule() {
  const [exams, setExams] = useState([]);
  const [duration, setDuration] = useState(180);

  useEffect(() => setExams(readPersonalSchedule()), []);

  const removeExam = (exam) => setExams(removeExamFromPersonalSchedule(exam, exam.enrollmentNumber));

  return (
    <Fade in timeout={600}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)', color: 'white', py: { xs: 4, md: 5 } }}>
          <Container maxWidth="lg">
            <Button component={Link} to="/" startIcon={<ArrowBackIcon />} sx={{ color: 'rgba(255,255,255,0.7)', mb: 2 }}>
              Back to Search
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <CalendarMonthIcon sx={{ fontSize: 32, color: '#FF8F00' }} />
              <Typography variant="h3" sx={{ fontWeight: 800, fontSize: { xs: '1.5rem', md: '2.2rem' } }}>
                My Exam Schedule
              </Typography>
            </Box>
            <Typography sx={{ opacity: 0.7, mt: 1 }}>Choose exams from your student schedule and export them to your calendar.</Typography>
          </Container>
        </Box>
        <Box sx={{ background: '#1A1A1A', minHeight: '55vh' }}>
          <Container maxWidth="lg" sx={{ py: { xs: 4, md: 5 } }}>
            {exams.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 8 }}>
                <EventBusyIcon sx={{ fontSize: 64, color: '#B0B0B0', opacity: 0.35, mb: 2 }} />
                <Typography variant="h5" sx={{ color: '#FFFFFF', mb: 1 }}>Your personal schedule is empty</Typography>
                <Typography sx={{ color: '#B0B0B0', mb: 3 }}>Search by enrollment number or browse exams, then add the ones you want to keep here.</Typography>
                <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1.5, flexWrap: 'wrap' }}>
                  <Button component={Link} to="/" variant="outlined" startIcon={<ArrowBackIcon />}>Search by enrollment</Button>
                  <Button component={Link} to="/exams" variant="contained" startIcon={<CalendarMonthIcon />}>Browse exams</Button>
                </Box>
              </Box>
            ) : (
              <>
                <Box sx={{ display: 'flex', alignItems: { xs: 'stretch', sm: 'center' }, justifyContent: 'space-between', gap: 2, flexWrap: 'wrap', mb: 3 }}>
                  <Typography variant="h5" sx={{ color: '#FFB300', fontWeight: 700 }}>{exams.length} saved exam{exams.length === 1 ? '' : 's'}</Typography>
                  <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>
                    <TextField select size="small" label="Fallback duration" value={duration} onChange={(event) => setDuration(Number(event.target.value))} sx={{ minWidth: 170 }}>
                      {[60, 90, 120, 180, 240].map((minutes) => <MenuItem key={minutes} value={minutes}>{minutes / 60} hour{minutes === 60 ? '' : 's'}</MenuItem>)}
                    </TextField>
                    <Button variant="contained" startIcon={<DownloadIcon />} onClick={() => downloadCalendar(exams, duration)}>
                      Export .ics calendar
                    </Button>
                  </Box>
                </Box>
                <Alert severity="info" sx={{ mb: 3 }}>Calendar events use imported or saved end times when available. The selected fallback duration applies to exams without an end time. Events use India Standard Time.</Alert>
                <Grid container spacing={3}>
                  {exams.map((exam) => (
                    <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={`${exam.enrollmentNumber}-${exam.subjectCode}-${exam.examDate}-${exam.examTime}`}>
                      <ExamCard exam={exam} selected onToggle={() => removeExam(exam)} enrollmentNumber={exam.enrollmentNumber} />
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
