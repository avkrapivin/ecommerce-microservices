import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance with credentials
const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

export interface LoginResponse {
  message: string;
  user: UserProfile;
}

export interface RegisterResponse {
  message: string;
  userId: string;
}

// Auth API functions
export const authApi = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post('/users/login', credentials);
    return response.data;
  },

  async register(userData: RegisterRequest): Promise<RegisterResponse> {
    const response = await api.post('/users/register', userData);
    return response.data;
  },

  async logout(): Promise<void> {
    await api.post('/users/logout');
  },

  async getProfile(): Promise<UserProfile> {
    const response = await api.get('/users/profile');
    return response.data;
  },

  async updateProfile(profileData: Partial<UserProfile>): Promise<UserProfile> {
    const response = await api.put('/users/profile', profileData);
    return response.data;
  },

  async confirmEmail(email: string, code: string): Promise<{ message: string }> {
    const response = await api.post('/users/confirm', { email, confirmationCode: code });
    return response.data;
  },

  async resendConfirmationCode(email: string): Promise<{ message: string }> {
    const response = await api.post('/users/resend-code', { email });
    return response.data;
  },

  async resetPassword(email: string): Promise<{ message: string }> {
    const response = await api.post('/users/reset-password', { email });
    return response.data;
  },

  async confirmPasswordReset(email: string, code: string, newPassword: string): Promise<{ message: string }> {
    const response = await api.post('/users/confirm-reset', { email, code, newPassword });
    return response.data;
  },
};

// Export individual functions for backward compatibility
export const {
  login,
  register,
  logout,
  getProfile,
  updateProfile,
  confirmEmail,
  resendConfirmationCode,
  resetPassword,
  confirmPasswordReset,
} = authApi;
