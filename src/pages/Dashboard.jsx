import { Link } from 'react-router-dom';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Button from '@mui/material/Button';
import Fade from '@mui/material/Fade';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import PeopleIcon from '@mui/icons-material/People';
import QuizIcon from '@mui/icons-material/Quiz';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import DashboardIcon from '@mui/icons-material/Dashboard';

const dashboardCards = [
  {
    title: 'Departments',
    description: 'Manage academic departments. Add, edit, or remove department records.',
    icon: AccountBalanceIcon,
    path: '/admin/departments',
    color: '#1565C0',
  },
  {
    title: 'Subjects',
    description: 'Manage course subjects. Configure subject codes, names, and semester assignments.',
    icon: MenuBookIcon,
    path: '/admin/subjects',
    color: '#2E7D32',
  },
  {
    title: 'Students',
    description: 'Manage student records. Handle enrollment, department assignments, and credentials.',
    icon: PeopleIcon,
    path: '/admin/students',
    color: '#E65100',
  },
  {
    title: 'Exams',
    description: 'Schedule and manage examinations. Set dates, times, rooms, and building assignments.',
    icon: QuizIcon,
    path: '/admin/exams',
    color: '#7B1FA2',
  },
];

export default function Dashboard() {
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
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
              <DashboardIcon sx={{ fontSize: 32 }} />
              <Typography
                variant="h3"
                sx={{ fontWeight: 700, fontSize: { xs: '1.5rem', md: '2rem' } }}
              >
                Admin Dashboard
              </Typography>
            </Box>
            <Typography variant="body1" sx={{ opacity: 0.8 }}>
              Manage your university examination system
            </Typography>
          </Container>
        </Box>

        {/* Cards Grid */}
        <Container maxWidth="lg" sx={{ py: { xs: 4, md: 5 } }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                sm: 'repeat(2, 1fr)',
                lg: 'repeat(4, 1fr)',
              },
              gap: 3,
            }}
          >
            {dashboardCards.map((card) => {
              const IconComp = card.icon;
              return (
                <Card
                  key={card.title}
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    position: 'relative',
                    overflow: 'visible',
                  }}
                >
                  {/* Color accent */}
                  <Box
                    sx={{
                      height: 4,
                      backgroundColor: card.color,
                      borderRadius: '16px 16px 0 0',
                    }}
                  />
                  <CardContent sx={{ p: 3, flexGrow: 1 }}>
                    <Box
                      sx={{
                        width: 52,
                        height: 52,
                        borderRadius: 2.5,
                        backgroundColor: `${card.color}14`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        mb: 2,
                      }}
                    >
                      <IconComp sx={{ fontSize: 28, color: card.color }} />
                    </Box>
                    <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
                      {card.title}
                    </Typography>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ lineHeight: 1.6 }}
                    >
                      {card.description}
                    </Typography>
                  </CardContent>
                  <CardActions sx={{ px: 3, pb: 2.5 }}>
                    <Button
                      component={Link}
                      to={card.path}
                      variant="contained"
                      endIcon={<ArrowForwardIcon />}
                      fullWidth
                      sx={{
                        backgroundColor: card.color,
                        '&:hover': {
                          backgroundColor: card.color,
                          filter: 'brightness(0.9)',
                        },
                      }}
                    >
                      Manage
                    </Button>
                  </CardActions>
                </Card>
              );
            })}
          </Box>
        </Container>
      </Box>
    </Fade>
  );
}
