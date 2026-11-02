// Fields marked "admin only" are left out of the responses sent to sellers

export interface Product {
  id?: number;
  productName: string;
  provider: string;
  productQuantity: number;
  category: string;
  price: number;
  costPrice: number; // admin only
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
  spentAmount?: number; // admin only
  deleted?: boolean;
}

export interface Sale {
  id?: number;
  product: Product;
  client?: Client;
  quantity: number;
  totalAmount: number; // admin only
  costAmount: number; // admin only
  profitAmount: number; // admin only
  saleDate: string;
  invoiceId?: number;
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
  invoiceNumber?: string;
  carModel?: string;
  invoiceId?: number;
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
  invoiceNumber?: string;
  carModel?: string;
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
  productId?: number; // empty on service lines, such as a billed installation
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
  installationRevenue: number;
  installationCount: number;
}

export interface SalesByDay { date: string; total: number; }
export interface SalesByMonth { month: string; total: number; }
export interface TopProduct { productId: number; productName: string; quantitySold: number; revenue: number; }
export interface CategorySales { category: string; revenue: number; quantity: number; }
export interface LowStockProduct { productId: number; productName: string; quantity: number; category: string; }
export interface RecentSale { saleId: number; productName: string; quantity: number; totalAmount: number; saleDate: string; }
export interface DailyAnalytics { date: string; revenue: number; cost: number; profit: number; margin: number; salesCount: number; itemsSold: number; }
