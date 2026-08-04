import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import InputAdornment from '@mui/material/InputAdornment';
import SearchIcon from '@mui/icons-material/Search';
import BadgeIcon from '@mui/icons-material/Badge';

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
    navigate(`/search/${encodeURIComponent(trimmed)}`);
  };

  return (
    <Box
      component="form"
      onSubmit={handleSearch}
      sx={{
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' },
        gap: 1.5,
        width: '100%',
        maxWidth: 560,
      }}
      role="search"
      aria-label="Search exam schedule"
    >
      <TextField
        id="enrollment-search-input"
        fullWidth
        placeholder="e.g. RBU2024001"
        label="Enrollment Number"
        value={enrollment}
        onChange={(e) => {
          setEnrollment(e.target.value);
          if (error) setError('');
        }}
        error={!!error}
        helperText={error}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <BadgeIcon color="action" />
              </InputAdornment>
            ),
          },
          htmlInput: {
            'aria-label': 'Enrollment number',
          },
        }}
        sx={{
          '& .MuiOutlinedInput-root': {
            backgroundColor: 'white',
            boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
          },
        }}
      />
      <Button
        id="search-schedule-button"
        type="submit"
        variant="contained"
        color="secondary"
        startIcon={<SearchIcon />}
        sx={{
          minWidth: { xs: '100%', sm: 160 },
          height: 56,
          fontSize: '1rem',
          boxShadow: '0 4px 14px rgba(25, 118, 210, 0.35)',
        }}
        aria-label="Search exam schedule"
      >
        Search
      </Button>
    </Box>
  );
}
