import api from './api';
import { Sale } from '../types';

export const saleService = {
  getAll: async (): Promise<Sale[]> => {
    const response = await api.get<Sale[]>('/api/sales');
    return response.data;
  },

  create: async (productId: number, quantity: number, idempotencyKey: string): Promise<Sale> => {
    const response = await api.post<Sale>(`/api/sales?productId=${productId}&quantity=${quantity}`, undefined, {
      headers: { 'Idempotency-Key': idempotencyKey },
    });
    return response.data;
  },

  createWithClient: async (productId: number, clientId: number, quantity: number, idempotencyKey: string): Promise<Sale> => {
    const response = await api.post<Sale>(
      `/api/sales/with-client?productId=${productId}&clientId=${clientId}&quantity=${quantity}`,
      undefined,
      { headers: { 'Idempotency-Key': idempotencyKey } }
    );
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/sales/${id}`);
  },

  getByClient: async (clientId: number): Promise<Sale[]> => {
    const response = await api.get<Sale[]>(`/api/sales/client/${clientId}`);
    return response.data;
  },

  getCountByClient: async (clientId: number): Promise<number> => {
    const response = await api.get<number>(`/api/sales/client/${clientId}/count`);
    return response.data;
  },

  getTotalCount: async (): Promise<number> => {
    const response = await api.get<number>('/api/sales/count');
    return response.data;
  },

  getCountByProduct: async (productId: number): Promise<number> => {
    const response = await api.get<number>(`/api/sales/count/${productId}`);
    return response.data;
  },
};
