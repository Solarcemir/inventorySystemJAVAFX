import api from './api';
import { LoginResponse } from '../types';

export const authService = {
  login: async (username: string, password: string): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/api/login', { username, password });
    return response.data;
  },
};

// Helper to get current user info from sessionStorage
export function getCurrentUser() {
  return {
    username: sessionStorage.getItem('username') || '',
    role: sessionStorage.getItem('role') || 'ADMIN',
  };
}

export function isAdmin() {
  return getCurrentUser().role === 'ADMIN';
}

export function isSeller() {
  return getCurrentUser().role === 'SELLER';
}
