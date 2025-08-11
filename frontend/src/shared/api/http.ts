import axios from 'axios';

// Read API base URL from environment or fallback to localhost
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export const http = axios.create({
  baseURL,
  withCredentials: true, // send cookies (HttpOnly) with requests
});

// Simple 401 handling: redirect to login page
http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error?.response?.status === 401) {
      // Invalidate client state and redirect to login
      if (window.location.pathname !== '/auth/login') {
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  },
);
