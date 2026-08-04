import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import Fade from '@mui/material/Fade';

export default function Loading({ message = 'Loading...' }) {
  return (
    <Fade in timeout={400}>
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          py: 10,
          gap: 2.5,
        }}
      >
        <CircularProgress
          size={48}
          thickness={4}
          sx={{ color: 'secondary.main' }}
        />
        <Typography
          variant="body1"
          color="text.secondary"
          sx={{ fontWeight: 500 }}
        >
          {message}
        </Typography>
      </Box>
    </Fade>
  );
}
