import { lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import theme from './theme/theme';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import Loading from './components/Loading';

const Home = lazy(() => import('./pages/Home'));
const SearchResult = lazy(() => import('./pages/SearchResult'));
const BrowseExams = lazy(() => import('./pages/BrowseExams'));
const AdminLogin = lazy(() => import('./pages/AdminLogin'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Departments = lazy(() => import('./pages/Departments'));
const Subjects = lazy(() => import('./pages/Subjects'));
const Students = lazy(() => import('./pages/Students'));
const Exams = lazy(() => import('./pages/Exams'));
const AdminSetup = lazy(() => import('./pages/AdminSetup'));
const MySchedule = lazy(() => import('./pages/MySchedule'));
const SeatingLookup = lazy(() => import('./pages/SeatingLookup'));
const SeatingPlans = lazy(() => import('./pages/SeatingPlans'));
const NotFound = lazy(() => import('./pages/NotFound'));

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            minHeight: '100vh',
          }}
        >
          <Box
            component="a"
            href="#main-content"
            sx={{
              position: 'fixed',
              top: 8,
              left: 8,
              zIndex: (appTheme) => appTheme.zIndex.tooltip + 1,
              px: 2,
              py: 1,
              borderRadius: 1,
              backgroundColor: '#FFB300',
              color: '#1A1A1A',
              fontWeight: 700,
              textDecoration: 'none',
              transform: 'translateY(-200%)',
              transition: 'transform 0.15s ease',
              '&:focus': { transform: 'translateY(0)' },
            }}
          >
            Skip to main content
          </Box>

          <Navbar />

          <Box
            component="main"
            id="main-content"
            tabIndex={-1}
            sx={{ flexGrow: 1, '&:focus': { outline: 'none' } }}
          >
            <Suspense fallback={<Loading message="Loading page..." />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/" element={<Home />} />
                <Route path="/search/:enrollmentNumber" element={<SearchResult />} />
                <Route path="/exams" element={<BrowseExams />} />
                <Route path="/calendar" element={<MySchedule />} />
                <Route path="/seating" element={<SeatingLookup />} />

                {/* Auth Route */}
                <Route path="/admin/login" element={<AdminLogin />} />
                <Route path="/admin/setup" element={<AdminSetup />} />

                {/* Protected Admin Routes */}
                <Route
                  path="/admin/dashboard"
                  element={
                    <ProtectedRoute>
                      <Dashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/departments"
                  element={
                    <ProtectedRoute>
                      <Departments />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/subjects"
                  element={
                    <ProtectedRoute>
                      <Subjects />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/students"
                  element={
                    <ProtectedRoute>
                      <Students />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/exams"
                  element={
                    <ProtectedRoute>
                      <Exams />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/admin/seating"
                  element={
                    <ProtectedRoute>
                      <SeatingPlans />
                    </ProtectedRoute>
                  }
                />
                <Route path="*" element={<NotFound />} />
              </Routes>
            </Suspense>
          </Box>

          <Footer />
        </Box>
      </Router>
    </ThemeProvider>
  );
}
