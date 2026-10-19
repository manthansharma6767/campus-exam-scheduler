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
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';

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

export default function ExamCard({ exam, selected = false, onToggle, enrollmentNumber }) {
  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        overflow: 'visible',
        backgroundColor: '#2A2A2A',
        border: '1px solid rgba(255,255,255,0.06)',
        '&:hover': {
          borderColor: 'rgba(230, 81, 0, 0.3)',
          boxShadow: '0 8px 30px rgba(230, 81, 0, 0.12)',
        },
      }}
    >
      {/* Accent top border — orange gradient */}
      <Box
        sx={{
          height: 4,
          background: 'linear-gradient(90deg, #E65100, #FF8F00)',
          borderRadius: '16px 16px 0 0',
        }}
      />
      <CardContent sx={{ p: 3, flexGrow: 1 }}>
        {/* Subject header */}
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 2 }}>
          <MenuBookIcon sx={{ color: '#FF8F00', fontSize: 28, mt: 0.3 }} />
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, lineHeight: 1.3, color: '#FFFFFF' }}>
              {exam.subjectName}
            </Typography>
            <Chip
              label={exam.subjectCode}
              size="small"
              variant="outlined"
              sx={{
                mt: 0.5,
                fontWeight: 600,
                borderColor: '#FF8F00',
                color: '#FFB300',
              }}
            />
          </Box>
        </Box>

        <Divider sx={{ mb: 2, borderColor: 'rgba(255,255,255,0.06)' }} />

        {/* Details grid */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <CalendarMonthIcon sx={{ color: '#B0B0B0', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" sx={{ display: 'block', color: '#888' }}>
                Date
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#E0E0E0' }}>
                {formatDate(exam.examDate)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <AccessTimeIcon sx={{ color: '#B0B0B0', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" sx={{ display: 'block', color: '#888' }}>
                Time
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#E0E0E0' }}>
                {formatTime(exam.examTime)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <LocationOnIcon sx={{ color: '#B0B0B0', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" sx={{ display: 'block', color: '#888' }}>
                Building
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#E0E0E0' }}>
                {exam.building}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <MeetingRoomIcon sx={{ color: '#B0B0B0', fontSize: 20 }} />
            <Box>
              <Typography variant="caption" sx={{ display: 'block', color: '#888' }}>
                Room
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#E0E0E0' }}>
                {exam.roomNumber}
              </Typography>
            </Box>
          </Box>
        </Box>
      </CardContent>
      {onToggle && (
        <Box sx={{ px: 2, pb: 1 }}>
          <FormControlLabel
            control={<Checkbox checked={selected} onChange={onToggle} sx={{ color: '#FF8F00', '&.Mui-checked': { color: '#FF8F00' } }} />}
            label={selected ? 'In my schedule' : 'Add to my schedule'}
            slotProps={{ typography: { sx: { color: '#E0E0E0' } } }}
          />
          {enrollmentNumber && <Typography variant="caption" sx={{ display: 'block', color: '#888', pl: 1.5 }}>Enrollment {enrollmentNumber}</Typography>}
        </Box>
      )}
    </Card>
  );
}
