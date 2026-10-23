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
          <Navbar />

          <Box component="main" sx={{ flexGrow: 1 }}>
            <Suspense fallback={<Loading message="Loading page..." />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/" element={<Home />} />
                <Route path="/search/:enrollmentNumber" element={<SearchResult />} />
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
