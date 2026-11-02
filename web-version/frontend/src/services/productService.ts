import api from './api';
import { Product } from '../types';

// Ignores case, accents and separators, so "KD-X35" is also found as "kdx35"
const normalize = (text: string) =>
  text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');

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

  /**
   * True when every word typed appears somewhere in the product, in any position
   * and order: "x32d" and "x32d pionner" both find "Pionner bocina x32D".
   */
  matchesSearch: (product: Product, query: string): boolean => {
    const fields = [
      product.productName,
      product.category,
      product.provider,
      product.description,
      product.location,
      String(product.id),
    ].map((field) => normalize(field || ''));

    return query
      .split(/\s+/)
      .map(normalize)
      .filter(Boolean)
      .every((word) => fields.some((field) => field.includes(word)));
  },
};
