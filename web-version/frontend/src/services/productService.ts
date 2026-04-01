import api from './api';
import { Product } from '../types';

export const productService = {
  getAll: async (): Promise<Product[]> => {
    const response = await api.get<Product[]>('/api/products');
    return response.data;
  },

  getById: async (id: number): Promise<Product> => {
    const response = await api.get<Product>(`/api/products/${id}`);
    return response.data;
  },

  create: async (product: Product): Promise<Product> => {
    const response = await api.post<Product>('/api/products', product);
    return response.data;
  },

  update: async (id: number, product: Product): Promise<Product> => {
    const response = await api.put<Product>(`/api/products/${id}`, product);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/products/${id}`);
  },

  getCount: async (): Promise<number> => {
    const response = await api.get<number>('/api/products/count');
    return response.data;
  },
};
