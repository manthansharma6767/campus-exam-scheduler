import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', minHeight: '65vh', background: '#1A1A1A', textAlign: 'center' }}>
      <Container maxWidth="sm">
        <Typography variant="h1" sx={{ color: '#FF8F00', fontWeight: 800, fontSize: { xs: '5rem', md: '7rem' } }}>
          404
        </Typography>
        <Typography variant="h4" sx={{ color: '#FFFFFF', fontWeight: 700, mb: 1 }}>
          Page not found
        </Typography>
        <Typography sx={{ color: '#B0B0B0', mb: 3 }}>
          That page may have moved, or the address may be incorrect.
        </Typography>
        <Button component={Link} to="/" variant="contained">
          Return to home
        </Button>
      </Container>
    </Box>
  );
}
