import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Container from '@mui/material/Container';
import Fade from '@mui/material/Fade';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import SearchIcon from '@mui/icons-material/Search';
import ChairIcon from '@mui/icons-material/Chair';
import PlaceIcon from '@mui/icons-material/Place';
import SchoolIcon from '@mui/icons-material/School';
import api from '../services/api';

export default function SeatingLookup() {
  const [plans, setPlans] = useState([]);
  const [planId, setPlanId] = useState('');
  const [rollNumber, setRollNumber] = useState('');
  const [seat, setSeat] = useState(null);
  const [loadingPlans, setLoadingPlans] = useState(true);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/seating/plans')
      .then(({ data }) => {
        setPlans(data);
        if (data.length) setPlanId(String(data[0].seatingPlanId));
      })
      .catch(() => setError('Could not load seating plans. Please try again later.'))
      .finally(() => setLoadingPlans(false));
  }, []);

  const handleSearch = async (event) => {
    event.preventDefault();
    const roll = rollNumber.trim();
    if (!roll) {
      setError('Enter your roll number, for example A1-16.');
      setSeat(null);
      return;
    }
    if (!planId) {
      setError('There is no seating plan available yet.');
      setSeat(null);
      return;
    }
    setSearching(true);
    setError('');
    setSeat(null);
    try {
      const { data } = await api.get(`/api/seating/plans/${planId}/roll/${encodeURIComponent(roll)}`);
      setSeat(data);
    } catch (requestError) {
      const status = requestError.response?.status;
      setError(status === 404
        ? `No seat was found for ${roll.toUpperCase()} in this plan. Check the roll number or choose another plan.`
        : requestError.response?.data?.message || 'Could not search this roll number. Please try again.');
    } finally {
      setSearching(false);
    }
  };

  const selectedPlan = plans.find((plan) => String(plan.seatingPlanId) === String(planId));

  return (
    <Fade in timeout={500}>
      <Box>
        <Box sx={{ background: 'linear-gradient(135deg, #1A1A1A 0%, #2D1500 50%, #4A1800 100%)', color: 'white', py: { xs: 5, md: 7 } }}>
          <Container maxWidth="md">
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
              <ChairIcon sx={{ color: '#FF8F00', fontSize: 34 }} />
              <Typography variant="h3" sx={{ fontWeight: 800, fontSize: { xs: '1.8rem', md: '2.5rem' } }}>
                Find My Seat
              </Typography>
            </Box>
            <Typography sx={{ color: 'rgba(255,255,255,0.72)' }}>
              Enter your roll number to find your examination room and seat position.
            </Typography>
          </Container>
        </Box>

        <Box sx={{ background: '#1A1A1A', minHeight: '60vh', py: { xs: 4, md: 6 } }}>
          <Container maxWidth="md">
            {loadingPlans ? (
              <Alert severity="info">Loading available seating plans…</Alert>
            ) : plans.length === 0 && error ? (
              <Alert severity="error">{error}</Alert>
            ) : plans.length === 0 ? (
              <Card sx={{ backgroundColor: '#242424', border: '1px solid rgba(255,255,255,0.08)' }}>
                <CardContent sx={{ p: { xs: 3, md: 5 }, textAlign: 'center' }}>
                  <ChairIcon sx={{ color: '#FF8F00', fontSize: 48, mb: 1 }} />
                  <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>No seating plan is available</Typography>
                  <Typography sx={{ color: '#B0B0B0', mb: 3 }}>
                    The exam office has not uploaded a seating arrangement yet.
                  </Typography>
                  {localStorage.getItem('token') && (
                    <Button component={Link} to="/admin/seating" variant="contained">Open seating plan management</Button>
                  )}
                </CardContent>
              </Card>
            ) : (
              <>
                <Card sx={{ backgroundColor: '#242424', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 3 }}>
                  <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
                    <Box component="form" onSubmit={handleSearch} noValidate>
                      <TextField
                        select
                        fullWidth
                        label="Seating plan"
                        value={planId}
                        onChange={(event) => { setPlanId(event.target.value); setSeat(null); setError(''); }}
                        sx={{ mb: 2.5 }}
                        slotProps={{ select: { inputProps: { 'aria-label': 'Choose a seating plan' } } }}
                      >
                        {plans.map((plan) => (
                          <MenuItem key={plan.seatingPlanId} value={String(plan.seatingPlanId)}>
                            {plan.title}{plan.semester ? ` · Semester ${plan.semester}` : ''}
                          </MenuItem>
                        ))}
                      </TextField>
                      <Box sx={{ display: 'flex', gap: 1.5, flexDirection: { xs: 'column', sm: 'row' } }}>
                        <TextField
                          fullWidth
                          label="Roll number"
                          placeholder="For example, A1-16"
                          value={rollNumber}
                          onChange={(event) => { setRollNumber(event.target.value); setError(''); }}
                          slotProps={{ htmlInput: { 'aria-label': 'Roll number', maxLength: 20, style: { textTransform: 'uppercase' } } }}
                          autoComplete="off"
                        />
                        <Button
                          type="submit"
                          variant="contained"
                          startIcon={<SearchIcon />}
                          disabled={searching}
                          sx={{ minWidth: { sm: 170 }, py: 1.5, whiteSpace: 'nowrap' }}
                        >
                          {searching ? 'Searching…' : 'Find my seat'}
                        </Button>
                      </Box>
                    </Box>
                    {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
                  </CardContent>
                </Card>

                {seat && (
                  <Card sx={{ mt: 3, background: 'linear-gradient(135deg, #2A2A2A 0%, #352010 100%)', border: '1px solid rgba(255,143,0,0.35)', borderRadius: 3 }}>
                    <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                        <PlaceIcon sx={{ color: '#FF8F00' }} />
                        <Typography variant="overline" sx={{ color: '#FFB300', letterSpacing: '0.12em' }}>Your seating assignment</Typography>
                      </Box>
                      <Typography variant="h4" sx={{ fontWeight: 800, mb: 2 }}>
                        Room {seat.roomNumber}
                      </Typography>
                      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
                        <Box>
                          <Typography variant="caption" sx={{ color: '#B0B0B0' }}>Roll number</Typography>
                          <Typography sx={{ fontWeight: 700 }}>{seat.rollNumber}</Typography>
                        </Box>
                        <Box>
                          <Typography variant="caption" sx={{ color: '#B0B0B0' }}>Row from front</Typography>
                          <Typography sx={{ fontWeight: 700 }}>Row {seat.rowNumber}</Typography>
                        </Box>
                        <Box>
                          <Typography variant="caption" sx={{ color: '#B0B0B0' }}>Column</Typography>
                          <Typography sx={{ fontWeight: 700 }}>Column {seat.columnNumber}</Typography>
                        </Box>
                      </Box>
                      <Alert severity="info" sx={{ mt: 3 }} icon={<SchoolIcon />}>
                        Row 1 is closest to the blackboard/front; columns follow the left-to-right layout in the chart. Plan: {seat.planTitle}.
                      </Alert>
                    </CardContent>
                  </Card>
                )}
                {selectedPlan && !seat && !error && (
                  <Typography variant="body2" sx={{ color: '#8E8E8E', mt: 2, textAlign: 'center' }}>
                    {selectedPlan.seatCount} seats across {selectedPlan.roomCount} rooms
                  </Typography>
                )}
              </>
            )}
          </Container>
        </Box>
      </Box>
    </Fade>
  );
}
