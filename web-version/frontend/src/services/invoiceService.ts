import api from './api';
import { Invoice } from '../types';

export const invoiceService = {
  getAll: async (): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>('/api/invoices');
    return response.data;
  },

  getOpen: async (): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>('/api/invoices/open');
    return response.data;
  },

  getById: async (id: number): Promise<Invoice> => {
    const response = await api.get<Invoice>(`/api/invoices/${id}`);
    return response.data;
  },

  create: async (invoice: Partial<Invoice>): Promise<Invoice> => {
    const response = await api.post<Invoice>('/api/invoices', invoice);
    return response.data;
  },

  addItem: async (invoiceId: number, productId: number, quantity: number): Promise<Invoice> => {
    const response = await api.post<Invoice>(
      `/api/invoices/${invoiceId}/items?productId=${productId}&quantity=${quantity}`
    );
    return response.data;
  },

  removeItem: async (invoiceId: number, itemId: number): Promise<Invoice> => {
    const response = await api.delete<Invoice>(`/api/invoices/${invoiceId}/items/${itemId}`);
    return response.data;
  },

  updateSettings: async (invoiceId: number, settings: { invoiceName?: string; clientName?: string; discountPercent?: string }): Promise<Invoice> => {
    const response = await api.put<Invoice>(`/api/invoices/${invoiceId}/settings`, settings);
    return response.data;
  },

  finalize: async (invoiceId: number): Promise<Invoice> => {
    const response = await api.post<Invoice>(`/api/invoices/${invoiceId}/finalize`);
    return response.data;
  },

  cancel: async (invoiceId: number): Promise<void> => {
    await api.post(`/api/invoices/${invoiceId}/cancel`);
  },
};
