import { useEffect, useState, useMemo } from 'react';
import { saleService } from '../services/saleService';
import { isAdmin } from '../services/authService';
import { getErrorMessage } from '../services/api';
import { Sale } from '../types';

export default function Sales() {
  const admin = isAdmin();
  const [sales, setSales] = useState<Sale[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterMonth, setFilterMonth] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { loadSales(); }, []);

  const loadSales = async () => {
    try {
      setSales(await saleService.getAll());
    } catch (err) {
      console.error('Failed to load sales', err);
    } finally {
      setLoading(false);
    }
  };

  // Extract unique months from sales
  const months = useMemo(() => {
    const set = new Set<string>();
    sales.forEach((s) => {
      const d = new Date(s.saleDate);
      if (!isNaN(d.getTime())) {
        const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        set.add(key);
      }
    });
    return [...set].sort().reverse();
  }, [sales]);

  const formatMonthLabel = (key: string) => {
    const [y, m] = key.split('-');
    const date = new Date(parseInt(y), parseInt(m) - 1);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });
  };

  // Filter by month
  const filtered = useMemo(() => {
    if (!filterMonth) return sales;
    return sales.filter((s) => {
      const d = new Date(s.saleDate);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      return key === filterMonth;
    });
  }, [sales, filterMonth]);

  // Summary for filtered sales
  const totalRevenue = filtered.reduce((sum, s) => sum + Number(s.totalAmount), 0);
  const totalProfit = filtered.reduce((sum, s) => sum + Number(s.profitAmount), 0);

  const handleDelete = async (sale: Sale) => {
    const msg = `Delete sale #${sale.id}?\n\nThis will return ${sale.quantity} unit(s) of "${sale.product?.productName}" back to inventory.`;
    if (!window.confirm(msg)) return;
    setError('');
    try {
      await saleService.delete(sale.id!);
      await loadSales();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to delete sale'));
    }
  };

  if (loading) return <div className="page-loading">Loading sales...</div>;

  return (
    <div className="page">
      <div className="page-title-row">
        <div className="page-title-icon sales-icon">$</div>
        <div>
          <h1>Sales History</h1>
          <p className="page-subtitle">All recorded transactions</p>
        </div>
      </div>

      <div className="sales-toolbar">
        <div className="sales-filter-group">
          <label>Filter by Month:</label>
          <select value={filterMonth} onChange={(e) => setFilterMonth(e.target.value)}>
            <option value="">All Months</option>
            {months.map((m) => (
              <option key={m} value={m}>{formatMonthLabel(m)}</option>
            ))}
          </select>
        </div>
        <div className="sales-summary">
          <span>Showing: <strong>{filtered.length}</strong> sales</span>
          <span className="sales-summary-divider">|</span>
          <span>Revenue: <strong className="text-accent">${totalRevenue.toFixed(2)}</strong></span>
          {admin && (
            <>
              <span className="sales-summary-divider">|</span>
              <span>Profit: <strong className="text-success">${totalProfit.toFixed(2)}</strong></span>
            </>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {filtered.length === 0 ? (
        <p className="empty-state">
          {filterMonth ? 'No sales found for this month.' : 'No sales recorded yet. Go to Inventory to sell products.'}
        </p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Product</th>
              <th>Client</th>
              <th>Qty</th>
              <th>Total</th>
              {admin && <th>Cost</th>}
              {admin && <th>Profit</th>}
              <th>Date</th>
              {admin && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {filtered.map((s) => (
              <tr key={s.id}>
                <td>{s.id}</td>
                <td>{s.product?.productName || 'N/A'}</td>
                <td>{s.client ? `${s.client.firstName} ${s.client.lastName}` : 'Walk-in'}</td>
                <td>{s.quantity}</td>
                <td>${Number(s.totalAmount).toFixed(2)}</td>
                {admin && <td>${Number(s.costAmount).toFixed(2)}</td>}
                {admin && (
                  <td className={Number(s.profitAmount) >= 0 ? 'text-success' : 'text-danger'}>
                    ${Number(s.profitAmount).toFixed(2)}
                  </td>
                )}
                <td>{new Date(s.saleDate).toLocaleDateString()}</td>
                {admin && (
                  <td>
                    {s.invoiceId ? (
                      <span title="Reopen or delete the invoice to reverse this sale">Invoice #{s.invoiceId}</span>
                    ) : (
                      <button className="btn btn-sm btn-danger" onClick={() => handleDelete(s)} title="Delete sale and return to inventory">
                        Delete
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
