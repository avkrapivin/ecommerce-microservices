import axios from 'axios';

// Read API base URL from environment or fallback to localhost
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export const http = axios.create({
  baseURL,
  withCredentials: true, // send cookies (HttpOnly) with requests
});

// Simple 401 handling: redirect to login page for protected flows only
http.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error?.response?.status;
    const url: string = error?.config?.url ?? '';

    if (status === 401) {
      // Do NOT redirect on profile probe so public pages work unauthenticated
      if (url.includes('/users/profile')) {
        return Promise.reject(error);
      }
      // Redirect to login only if not already there
      if (window.location.pathname !== '/auth/login') {
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  },
);
