import api from './api';
import { Installation, Invoice } from '../types';

export const installationService = {
  getAll: async (): Promise<Installation[]> => {
    const response = await api.get<Installation[]>('/api/installations');
    return response.data;
  },

  getById: async (id: number): Promise<Installation> => {
    const response = await api.get<Installation>(`/api/installations/${id}`);
    return response.data;
  },

  create: async (installation: Installation): Promise<Installation> => {
    const response = await api.post<Installation>('/api/installations', installation);
    return response.data;
  },

  update: async (id: number, installation: Installation): Promise<Installation> => {
    const response = await api.put<Installation>(`/api/installations/${id}`, installation);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/installations/${id}`);
  },

  getByInvoiceId: async (invoiceId: number): Promise<Installation[]> => {
    const response = await api.get<Installation[]>(`/api/installations/by-invoice/${invoiceId}`);
    return response.data;
  },

  /** Returns the invoice billing this installation, opening one with its price if it has none. */
  createInvoice: async (id: number): Promise<Invoice> => {
    const response = await api.post<Invoice>(`/api/installations/${id}/invoice`);
    return response.data;
  },
};
