import api from './api';
import { Client } from '../types';

export const clientService = {
  getAll: async (): Promise<Client[]> => {
    const response = await api.get<Client[]>('/api/clients');
    return response.data;
  },

  getById: async (id: number): Promise<Client> => {
    const response = await api.get<Client>(`/api/clients/${id}`);
    return response.data;
  },

  create: async (client: Client): Promise<Client> => {
    const response = await api.post<Client>('/api/clients', client);
    return response.data;
  },

  update: async (id: number, client: Client): Promise<Client> => {
    const response = await api.put<Client>(`/api/clients/${id}`, client);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/clients/${id}`);
  },

  getMostLoyal: async (): Promise<Client> => {
    const response = await api.get<Client>('/api/clients/most-loyal');
    return response.data;
  },

  getCount: async (): Promise<number> => {
    const response = await api.get<number>('/api/clients/count');
    return response.data;
  },
};
