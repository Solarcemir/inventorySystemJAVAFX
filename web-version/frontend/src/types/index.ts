export interface Product {
  id?: number;
  productName: string;
  provider: string;
  productQuantity: number;
  category: string;
  price: number;
  costPrice: number;
  imagePath?: string;
  description?: string;
  location?: string;
  deleted?: boolean;
}

export interface Client {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  spentAmount?: number;
  deleted?: boolean;
}

export interface Sale {
  id?: number;
  product: Product;
  client?: Client;
  quantity: number;
  totalAmount: number;
  costAmount: number;
  profitAmount: number;
  saleDate: string;
}

export interface User {
  id?: number;
  username: string;
  password?: string;
  role: string; // ADMIN | SELLER
}

export interface LoginResponse {
  success: boolean;
  username: string;
  role: string;
  id: number;
}

export interface Installation {
  id?: number;
  serviceName: string;
  clientName: string;
  installerName: string;
  description?: string;
  price: number;
  serviceDate?: string;
  status: string;
}

export interface ActivityLog {
  id?: number;
  action: string;
  description: string;
  username: string;
  timestamp: string;
}

export interface Invoice {
  id?: number;
  invoiceName?: string;
  clientName?: string;
  sellerUsername?: string;
  subtotal: number;
  discountPercent: number;
  discountAmount: number;
  taxPercent: number;
  taxAmount: number;
  total: number;
  status: string;
  createdAt?: string;
  finalizedAt?: string;
  items: InvoiceItem[];
}

export interface InvoiceItem {
  id?: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface AnalyticsSummary {
  totalProducts: number;
  totalSalesCount: number;
  totalRevenue: number;
  totalCost: number;
  totalProfit: number;
  profitMargin: number;
  averageSale: number;
  inventoryValue: number;
  inventoryCost: number;
  potentialProfit: number;
  lowStockCount: number;
  outOfStockCount: number;
  todaySales: number;
  todayProfit: number;
}

export interface SalesByDay { date: string; total: number; }
export interface SalesByMonth { month: string; total: number; }
export interface TopProduct { productName: string; totalQuantity: number; totalRevenue: number; }
export interface CategorySales { category: string; totalRevenue: number; totalQuantity: number; }
export interface LowStockProduct { id: number; productName: string; quantity: number; category: string; }
export interface RecentSale { id: number; productName: string; quantity: number; totalAmount: number; saleDate: string; }
export interface DailyAnalytics { date: string; revenue: number; cost: number; profit: number; margin: number; salesCount: number; itemsSold: number; }
