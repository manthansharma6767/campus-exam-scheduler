import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Public endpoints that do NOT require authentication
const PUBLIC_PATTERNS = [
  /^\/api\/students\/[^/]+\/schedule$/,
  /^\/api\/auth\//,  // covers /api/auth/login and /api/auth/register
];


function isPublicEndpoint(url) {
  return PUBLIC_PATTERNS.some((pattern) => pattern.test(url));
}

// Request interceptor — attach JWT for admin routes
api.interceptors.request.use(
  (config) => {
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      config.headers.delete('Content-Type');
    }
    if (!isPublicEndpoint(config.url)) {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — redirect to login on 401 from admin routes
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response &&
      error.response.status === 401 &&
      !isPublicEndpoint(error.config?.url)
    ) {
      localStorage.removeItem('token');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

export default api;
