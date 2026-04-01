import api from './api';
import { ActivityLog } from '../types';
import { getCurrentUser } from './authService';

export const logService = {
  getAll: async (): Promise<ActivityLog[]> => {
    const response = await api.get<ActivityLog[]>('/api/logs');
    return response.data;
  },

  log: async (action: string, description: string): Promise<void> => {
    try {
      const { username } = getCurrentUser();
      await api.post('/api/logs', { action, description, username });
    } catch {
      // Non-blocking: log failures should never break the main operation
    }
  },
};
