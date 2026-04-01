import api from './api';
import {
  AnalyticsSummary,
  SalesByDay,
  SalesByMonth,
  TopProduct,
  CategorySales,
  LowStockProduct,
  RecentSale,
  DailyAnalytics,
} from '../types';

export const analyticsService = {
  getSummary: async (): Promise<AnalyticsSummary> => {
    const response = await api.get<AnalyticsSummary>('/api/analytics/summary');
    return response.data;
  },

  getSalesByDay: async (): Promise<SalesByDay[]> => {
    const response = await api.get<SalesByDay[]>('/api/analytics/sales-by-day');
    return response.data;
  },

  getSalesByMonth: async (): Promise<SalesByMonth[]> => {
    const response = await api.get<SalesByMonth[]>('/api/analytics/sales-by-month');
    return response.data;
  },

  getTopProducts: async (): Promise<TopProduct[]> => {
    const response = await api.get<TopProduct[]>('/api/analytics/top-products');
    return response.data;
  },

  getSalesByCategory: async (): Promise<CategorySales[]> => {
    const response = await api.get<CategorySales[]>('/api/analytics/sales-by-category');
    return response.data;
  },

  getLowStockProducts: async (): Promise<LowStockProduct[]> => {
    const response = await api.get<LowStockProduct[]>('/api/analytics/low-stock');
    return response.data;
  },

  getRecentSales: async (): Promise<RecentSale[]> => {
    const response = await api.get<RecentSale[]>('/api/analytics/recent-sales');
    return response.data;
  },

  getDailyAnalytics: async (): Promise<DailyAnalytics[]> => {
    const response = await api.get<DailyAnalytics[]>('/api/analytics/daily');
    return response.data;
  },
};
