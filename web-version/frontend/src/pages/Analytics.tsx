import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { analyticsService } from '../services/analyticsService';
import {
  AnalyticsSummary, SalesByDay, TopProduct,
  CategorySales, LowStockProduct, RecentSale, DailyAnalytics,
} from '../types';

const PIE_COLORS = ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export default function Analytics() {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [salesByDay, setSalesByDay] = useState<SalesByDay[]>([]);
  const [topProducts, setTopProducts] = useState<TopProduct[]>([]);
  const [categoryData, setCategoryData] = useState<CategorySales[]>([]);
  const [lowStock, setLowStock] = useState<LowStockProduct[]>([]);
  const [recentSales, setRecentSales] = useState<RecentSale[]>([]);
  const [dailyData, setDailyData] = useState<DailyAnalytics[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadAnalytics(); }, []);

  const loadAnalytics = async () => {
    try {
      const [sum, byDay, top, byCat, low, recent, daily] = await Promise.all([
        analyticsService.getSummary(),
        analyticsService.getSalesByDay(),
        analyticsService.getTopProducts(),
        analyticsService.getSalesByCategory(),
        analyticsService.getLowStockProducts(),
        analyticsService.getRecentSales(),
        analyticsService.getDailyAnalytics(),
      ]);
      setSummary(sum);
      setSalesByDay(byDay);
      setTopProducts(top);
      setCategoryData(byCat);
      setLowStock(low);
      setRecentSales(recent);
      setDailyData(daily);
    } catch (err) {
      console.error('Failed to load analytics', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="page-loading">Loading analytics...</div>;
  if (!summary) return <div className="page-loading">No analytics data available</div>;

  return (
    <div className="page analytics-page">
      <h1 className="analytics-title">Analytics Dashboard</h1>

      {/* Row 1: Main KPIs */}
      <div className="analytics-kpi-row">
        <div className="akpi green"><span className="akpi-label">TOTAL REVENUE</span><span className="akpi-value">${Number(summary.totalRevenue).toFixed(2)}</span></div>
        <div className="akpi purple"><span className="akpi-label">TOTAL PROFIT</span><span className="akpi-value">${Number(summary.totalProfit).toFixed(2)}</span></div>
        <div className="akpi teal"><span className="akpi-label">PROFIT MARGIN</span><span className="akpi-value">{Number(summary.profitMargin).toFixed(2)}%</span></div>
        <div className="akpi blue"><span className="akpi-label">TODAY SALES</span><span className="akpi-value">${Number(summary.todaySales).toFixed(2)}</span></div>
        <div className="akpi dark"><span className="akpi-label">TODAY PROFIT</span><span className="akpi-value">${Number(summary.todayProfit).toFixed(2)}</span></div>
      </div>

      {/* Row 2: Secondary KPIs */}
      <div className="analytics-kpi-row">
        <div className="akpi-sm purple"><span className="akpi-sm-label">PRODUCTS</span><span className="akpi-sm-value">{summary.totalProducts}</span></div>
        <div className="akpi-sm orange"><span className="akpi-sm-label">TOTAL SALES</span><span className="akpi-sm-value">{summary.totalSalesCount}</span></div>
        <div className="akpi-sm teal"><span className="akpi-sm-label">AVG SALE</span><span className="akpi-sm-value">${Number(summary.averageSale).toFixed(2)}</span></div>
        <div className="akpi-sm gray"><span className="akpi-sm-label">INVENTORY VALUE</span><span className="akpi-sm-value">${Number(summary.inventoryValue).toFixed(2)}</span></div>
        <div className="akpi-sm warn"><span className="akpi-sm-label">LOW STOCK</span><span className="akpi-sm-value">{summary.lowStockCount}</span></div>
        <div className="akpi-sm red"><span className="akpi-sm-label">OUT OF STOCK</span><span className="akpi-sm-value">{summary.outOfStockCount}</span></div>
      </div>

      {/* Daily Analytics Table */}
      <div className="analytics-section">
        <h2>Daily Analytics (Last 7 Days)</h2>
        <table className="data-table">
          <thead>
            <tr><th>Date</th><th>Revenue</th><th>Cost</th><th>Profit</th><th>Margin %</th><th>Sales</th><th>Items</th></tr>
          </thead>
          <tbody>
            {dailyData.map((d, i) => (
              <tr key={i}>
                <td>{d.date}</td>
                <td>${Number(d.revenue).toFixed(2)}</td>
                <td>${Number(d.cost).toFixed(2)}</td>
                <td>${Number(d.profit).toFixed(2)}</td>
                <td>{Number(d.margin).toFixed(2)}%</td>
                <td>{d.salesCount}</td>
                <td>{d.itemsSold}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Charts Row */}
      <div className="analytics-charts-row">
        <div className="chart-card chart-accent">
          <h3>Sales Last 7 Days</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={salesByDay}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.2)" />
              <XAxis dataKey="date" tick={{ fill: '#333', fontSize: 12 }} />
              <YAxis tick={{ fill: '#333', fontSize: 12 }} />
              <Tooltip formatter={(v: number) => `$${v.toFixed(2)}`} />
              <Bar dataKey="total" fill="#6366f1" radius={[4, 4, 0, 0]} name="Revenue" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="chart-card">
          <h3>Sales by Category</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={categoryData} dataKey="totalRevenue" nameKey="category" cx="50%" cy="50%" outerRadius={90}
                label={({ category, percent }) => `${category} (${(percent * 100).toFixed(0)}%)`}>
                {categoryData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
              </Pie>
              <Tooltip formatter={(v: number) => `$${v.toFixed(2)}`} />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Bottom Tables */}
      <div className="analytics-tables-row">
        <div className="analytics-section">
          <h2>Top 5 Products</h2>
          <table className="data-table">
            <thead><tr><th>Product</th><th>Qty Sold</th><th>Revenue</th></tr></thead>
            <tbody>
              {topProducts.map((p, i) => (
                <tr key={i}>
                  <td>{p.productName}</td>
                  <td>{p.totalQuantity}</td>
                  <td>${Number(p.totalRevenue).toFixed(2)}</td>
                </tr>
              ))}
              {topProducts.length === 0 && <tr><td colSpan={3} className="text-center">No data</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="analytics-section">
          <h2>Recent Sales</h2>
          <table className="data-table">
            <thead><tr><th>Product</th><th>Qty</th><th>Total</th><th>Date</th></tr></thead>
            <tbody>
              {recentSales.map((s, i) => (
                <tr key={i}>
                  <td>{s.productName}</td>
                  <td>{s.quantity}</td>
                  <td>${Number(s.totalAmount).toFixed(2)}</td>
                  <td>{s.saleDate}</td>
                </tr>
              ))}
              {recentSales.length === 0 && <tr><td colSpan={4} className="text-center">No data</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="analytics-section">
          <h2>Low Stock Alert</h2>
          <table className="data-table">
            <thead><tr><th>Product</th><th>Category</th><th>Remaining</th></tr></thead>
            <tbody>
              {lowStock.map((p, i) => (
                <tr key={i}>
                  <td>{p.productName}</td>
                  <td>{p.category}</td>
                  <td className={p.quantity <= 2 ? 'text-danger' : 'text-warning'}>{p.quantity}</td>
                </tr>
              ))}
              {lowStock.length === 0 && <tr><td colSpan={3} className="text-center">All stocked</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
