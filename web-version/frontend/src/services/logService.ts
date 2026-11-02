import api from './api';
import { ActivityLog } from '../types';

export const logService = {
  getAll: async (): Promise<ActivityLog[]> => {
    const response = await api.get<ActivityLog[]>('/api/logs');
    return response.data;
  },
};
