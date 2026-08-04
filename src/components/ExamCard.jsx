import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import MeetingRoomIcon from '@mui/icons-material/MeetingRoom';
import MenuBookIcon from '@mui/icons-material/MenuBook';

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr + 'T00:00:00');
  return date.toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const [hours, minutes] = timeStr.split(':');
  const h = parseInt(hours, 10);
  const ampm = h >= 12 ? 'PM' : 'AM';
  const h12 = h % 12 || 12;
  return `${h12}:${minutes} ${ampm}`;
}

export default function ExamCard({ exam }) {
  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        overflow: 'visible',
      }}
    >
      {/* Accent top border */}
      <Box
        sx={{
          height: 4,
          background: 'linear-gradient(90deg, #1976D2, #42A5F5)',
          borderRadius: '16px 16px 0 0',
        }}
      />
      <CardContent sx={{ p: 3, flexGrow: 1 }}>
        {/* Subject header */}
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 2 }}>
          <MenuBookIcon sx={{ color: 'secondary.main', fontSize: 28, mt: 0.3 }} />
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, lineHeight: 1.3 }}>
              {exam.subjectName}
            </Typography>
            <Chip
              label={exam.subjectCode}
              size="small"
              color="secondary"
              variant="outlined"
              sx={{ mt: 0.5, fontWeight: 600 }}
            />
          </Box>
        </Box>

        <Divider sx={{ mb: 2 }} />

        {/* Details grid */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <CalendarMonthIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Date
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {formatDate(exam.examDate)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <AccessTimeIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Time
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {formatTime(exam.examTime)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <LocationOnIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Building
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {exam.building}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <MeetingRoomIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                Room
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {exam.roomNumber}
              </Typography>
            </Box>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}
