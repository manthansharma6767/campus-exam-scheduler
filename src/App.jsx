import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import theme from './theme/theme';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import SearchResult from './pages/SearchResult';
import AdminLogin from './pages/AdminLogin';
import Dashboard from './pages/Dashboard';
import Departments from './pages/Departments';
import Subjects from './pages/Subjects';
import Students from './pages/Students';
import Exams from './pages/Exams';

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
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<Home />} />
              <Route path="/search/:enrollmentNumber" element={<SearchResult />} />

              {/* Auth Route */}
              <Route path="/admin/login" element={<AdminLogin />} />

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
            </Routes>
          </Box>

          <Footer />
        </Box>
      </Router>
    </ThemeProvider>
  );
}
