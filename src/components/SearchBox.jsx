import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import SearchIcon from '@mui/icons-material/Search';
import Typography from '@mui/material/Typography';

export default function SearchBox() {
  const [enrollment, setEnrollment] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    const trimmed = enrollment.trim();
    if (!trimmed) {
      setError('Please enter your enrollment number');
      return;
    }
    setError('');

    // Save to search history
    try {
      const history = JSON.parse(localStorage.getItem('search_history') || '[]');
      const filtered = history.filter((item) => item.toUpperCase() !== trimmed.toUpperCase());
      const updated = [trimmed, ...filtered].slice(0, 5); // Keep top 5
      localStorage.setItem('search_history', JSON.stringify(updated));
    } catch {
      // Ignore storage errors
    }

    navigate(`/search/${encodeURIComponent(trimmed)}`);
  };

  return (
    <Box
      component="form"
      onSubmit={handleSearch}
      sx={{ width: '100%', maxWidth: 520 }}
      role="search"
      aria-label="Search exam schedule"
    >
      {/* Search bar matching McGill design — rounded dark with orange search button */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: 'rgba(255,255,255,0.06)',
          borderRadius: '50px',
          border: '1px solid rgba(255,255,255,0.12)',
          px: 2.5,
          py: 0.5,
          transition: 'all 0.3s ease',
          '&:focus-within': {
            borderColor: 'rgba(230, 81, 0, 0.5)',
            backgroundColor: 'rgba(255,255,255,0.08)',
            boxShadow: '0 0 20px rgba(230, 81, 0, 0.15)',
          },
        }}
      >
        <TextField
          id="enrollment-search-input"
          fullWidth
          placeholder="Search for exams (e.g. RBU2024001)"
          value={enrollment}
          onChange={(e) => {
            setEnrollment(e.target.value);
            if (error) setError('');
          }}
          variant="standard"
          slotProps={{
            input: {
              disableUnderline: true,
              sx: {
                color: '#FFFFFF',
                fontSize: '0.95rem',
                py: 1,
                '&::placeholder': {
                  color: 'rgba(255,255,255,0.4)',
                  opacity: 1,
                },
              },
            },
            htmlInput: {
              'aria-label': 'Enrollment number',
            },
          }}
        />
        <IconButton
          type="submit"
          aria-label="Search exam schedule"
          sx={{
            backgroundColor: '#E65100',
            color: '#FFFFFF',
            width: 42,
            height: 42,
            ml: 1,
            '&:hover': {
              backgroundColor: '#BF360C',
              transform: 'scale(1.05)',
            },
            transition: 'all 0.2s ease',
            boxShadow: '0 2px 8px rgba(230, 81, 0, 0.4)',
          }}
        >
          <SearchIcon />
        </IconButton>
      </Box>

      {/* Error message */}
      {error && (
        <Typography
          variant="caption"
          sx={{
            color: '#EF5350',
            mt: 1,
            ml: 2.5,
            display: 'block',
          }}
        >
          {error}
        </Typography>
      )}
    </Box>
  );
}
