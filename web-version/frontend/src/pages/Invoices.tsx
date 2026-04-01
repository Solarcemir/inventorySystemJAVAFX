import { useEffect, useState, useMemo, useRef } from 'react';
import { invoiceService } from '../services/invoiceService';
import { productService } from '../services/productService';
import { logService } from '../services/logService';
import { getCurrentUser } from '../services/authService';
import { Invoice, Product } from '../types';

export default function Invoices() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeInvoice, setActiveInvoice] = useState<Invoice | null>(null);
  const [error, setError] = useState('');

  // Add item state
  const [addProductId, setAddProductId] = useState(0);
  const [addProductName, setAddProductName] = useState('');
  const [addQty, setAddQty] = useState(1);
  const [productSearch, setProductSearch] = useState('');
  const [showProductDropdown, setShowProductDropdown] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // Settings state
  const [editName, setEditName] = useState('');
  const [editClient, setEditClient] = useState('');
  const [editDiscount, setEditDiscount] = useState('');
  const [showSettings, setShowSettings] = useState(false);

  const printRef = useRef<HTMLDivElement>(null);

  const { username } = getCurrentUser();

  // Close product dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowProductDropdown(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      const [invs, prods] = await Promise.all([invoiceService.getAll(), productService.getAll()]);
      setInvoices(invs);
      setProducts(prods);
      if (activeInvoice) {
        const updated = invs.find((i) => i.id === activeInvoice.id);
        if (updated) setActiveInvoice(updated);
        else setActiveInvoice(null);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const openInvoices = useMemo(() => invoices.filter((i) => i.status === 'OPEN'), [invoices]);
  const closedInvoices = useMemo(() => invoices.filter((i) => i.status !== 'OPEN'), [invoices]);

  const filteredProducts = useMemo(() => {
    if (!productSearch) return products;
    const q = productSearch.toLowerCase();
    return products.filter((p) =>
      p.productName.toLowerCase().includes(q) ||
      p.category.toLowerCase().includes(q) ||
      String(p.id).includes(q)
    );
  }, [products, productSearch]);

  const handleCreate = async () => {
    setError('');
    if (openInvoices.length >= 5) {
      setError('Maximum 5 open invoices allowed. Finalize or cancel one first.');
      return;
    }
    try {
      const inv = await invoiceService.create({ sellerUsername: username, invoiceName: `Invoice #${invoices.length + 1}` });
      await logService.log('INVOICE', `Created invoice #${inv.id}`);
      await loadData();
      setActiveInvoice(inv);
    } catch (err: any) {
      const msg = err.response?.data;
      setError(typeof msg === 'string' ? msg : msg?.message || 'Failed to create invoice');
    }
  };

  const handleAddItem = async () => {
    if (!activeInvoice || !addProductId) return;
    setError('');
    try {
      const updated = await invoiceService.addItem(activeInvoice.id!, addProductId, addQty);
      setActiveInvoice(updated);
      setAddProductId(0);
      setAddProductName('');
      setAddQty(1);
      setProductSearch('');
      setShowProductDropdown(false);
      await loadData();
    } catch (err: any) {
      setError(typeof err.response?.data === 'string' ? err.response.data : 'Failed to add item');
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (!activeInvoice) return;
    try {
      const updated = await invoiceService.removeItem(activeInvoice.id!, itemId);
      setActiveInvoice(updated);
      await loadData();
    } catch (err: any) {
      setError('Failed to remove item');
    }
  };

  const handleSaveSettings = async () => {
    if (!activeInvoice) return;
    try {
      const updated = await invoiceService.updateSettings(activeInvoice.id!, {
        invoiceName: editName,
        clientName: editClient,
        discountPercent: editDiscount,
      });
      setActiveInvoice(updated);
      setShowSettings(false);
      await loadData();
    } catch (err: any) {
      setError('Failed to update settings');
    }
  };

  const handleFinalize = async () => {
    if (!activeInvoice) return;
    if (!window.confirm('Finalize this invoice? This will deduct items from inventory and cannot be undone.')) return;
    try {
      const updated = await invoiceService.finalize(activeInvoice.id!);
      await logService.log('INVOICE', `Finalized invoice #${updated.id} — Total: $${Number(updated.total).toFixed(2)}`);
      setActiveInvoice(updated);
      await loadData();
    } catch (err: any) {
      setError(typeof err.response?.data === 'string' ? err.response.data : 'Failed to finalize');
    }
  };

  const handleCancel = async () => {
    if (!activeInvoice) return;
    if (!window.confirm('Cancel this invoice? Items will NOT be deducted from inventory.')) return;
    try {
      await invoiceService.cancel(activeInvoice.id!);
      await logService.log('INVOICE', `Cancelled invoice #${activeInvoice.id}`);
      setActiveInvoice(null);
      await loadData();
    } catch (err: any) {
      setError('Failed to cancel invoice');
    }
  };

  const handleExportPDF = () => {
    if (!printRef.current) return;
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;
    printWindow.document.write(`
      <html><head><title>${activeInvoice?.invoiceName || 'Invoice'}</title>
      <style>
        body { font-family: 'Segoe UI', sans-serif; padding: 40px; color: #333; }
        .inv-header { display: flex; justify-content: space-between; align-items: start; border-bottom: 3px solid #2c3e50; padding-bottom: 20px; margin-bottom: 20px; }
        .inv-brand { font-size: 28px; font-weight: 900; letter-spacing: 3px; }
        .inv-brand-sub { font-size: 12px; color: #777; }
        .inv-title { font-size: 24px; color: #2c3e50; text-align: right; }
        .inv-meta { text-align: right; font-size: 13px; color: #555; }
        .inv-meta span { display: block; margin-bottom: 4px; }
        .inv-client { margin-bottom: 20px; }
        .inv-client strong { color: #2c3e50; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        th { background: #2c3e50; color: #fff; padding: 10px 12px; text-align: left; font-size: 13px; }
        td { padding: 8px 12px; border-bottom: 1px solid #ddd; font-size: 13px; }
        .totals { width: 300px; margin-left: auto; }
        .totals tr td { border-bottom: 1px solid #eee; }
        .totals .grand-total td { border-top: 2px solid #2c3e50; font-size: 16px; font-weight: 700; }
        .inv-footer { text-align: center; margin-top: 40px; font-size: 12px; color: #999; border-top: 1px solid #eee; padding-top: 15px; }
        @media print { body { padding: 20px; } }
      </style></head><body>
      ${printRef.current.innerHTML}
      <div class="inv-footer">Thank you for your business! — CFT Electronic</div>
      </body></html>
    `);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => { printWindow.print(); }, 400);
  };

  const openSettingsFor = (inv: Invoice) => {
    setActiveInvoice(inv);
    setEditName(inv.invoiceName || '');
    setEditClient(inv.clientName || '');
    setEditDiscount(String(inv.discountPercent || 0));
    setShowSettings(true);
  };

  if (loading) return <div className="page-loading">Loading invoices...</div>;

  return (
    <div className="page invoice-page">
      <div className="page-title-row">
        <div className="page-title-icon" style={{ background: '#3498db' }}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
        </div>
        <div>
          <h1>Invoices & Receipts</h1>
          <p className="page-subtitle">Create, manage, and export invoices with Panama 7% ITBMS tax</p>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="invoice-layout">
        {/* Left: Invoice List + Create */}
        <div className="invoice-list-panel">
          <div className="invoice-list-header">
            <h2>Open Invoices ({openInvoices.length}/5)</h2>
            <button className="btn btn-primary" onClick={handleCreate} disabled={openInvoices.length >= 5}>
              + New Invoice
            </button>
          </div>

          {openInvoices.length === 0 ? (
            <p className="empty-state" style={{ padding: '1.5rem' }}>No open invoices. Create one to get started.</p>
          ) : (
            <div className="invoice-cards">
              {openInvoices.map((inv) => (
                <div
                  key={inv.id}
                  className={`invoice-card ${activeInvoice?.id === inv.id ? 'active' : ''}`}
                  onClick={() => { setActiveInvoice(inv); setShowSettings(false); setError(''); }}
                >
                  <div className="invoice-card-top">
                    <strong>{inv.invoiceName || `Invoice #${inv.id}`}</strong>
                    <span className="status-badge status-pending">OPEN</span>
                  </div>
                  <div className="invoice-card-meta">
                    <span>{inv.items?.length || 0} items</span>
                    <span className="invoice-card-total">${Number(inv.total).toFixed(2)}</span>
                  </div>
                  {inv.clientName && <div className="invoice-card-client">Client: {inv.clientName}</div>}
                </div>
              ))}
            </div>
          )}

          {closedInvoices.length > 0 && (
            <>
              <h3 className="invoice-history-title">History</h3>
              <div className="invoice-cards">
                {closedInvoices.slice(0, 10).map((inv) => (
                  <div
                    key={inv.id}
                    className={`invoice-card closed ${activeInvoice?.id === inv.id ? 'active' : ''}`}
                    onClick={() => { setActiveInvoice(inv); setShowSettings(false); }}
                  >
                    <div className="invoice-card-top">
                      <strong>{inv.invoiceName || `Invoice #${inv.id}`}</strong>
                      <span className={`status-badge ${inv.status === 'FINALIZED' ? 'status-completed' : 'status-cancelled'}`}>
                        {inv.status}
                      </span>
                    </div>
                    <div className="invoice-card-meta">
                      <span>{inv.items?.length || 0} items</span>
                      <span className="invoice-card-total">${Number(inv.total).toFixed(2)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        {/* Right: Active Invoice Detail */}
        <div className="invoice-detail-panel">
          {activeInvoice ? (
            <>
              <div className="invoice-detail-header">
                <div>
                  <h2>{activeInvoice.invoiceName || `Invoice #${activeInvoice.id}`}</h2>
                  <span className="invoice-detail-meta">
                    {activeInvoice.clientName && `Client: ${activeInvoice.clientName} · `}
                    Seller: {activeInvoice.sellerUsername} ·
                    Created: {activeInvoice.createdAt ? new Date(activeInvoice.createdAt).toLocaleDateString() : '—'}
                  </span>
                </div>
                <span className={`status-badge ${activeInvoice.status === 'OPEN' ? 'status-pending' : activeInvoice.status === 'FINALIZED' ? 'status-completed' : 'status-cancelled'}`}>
                  {activeInvoice.status}
                </span>
              </div>

              {/* Add Item (only for OPEN invoices) */}
              {activeInvoice.status === 'OPEN' && (
                <div className="invoice-add-item">
                  <h3>Add Product to Cart</h3>
                  <div className="invoice-add-row">
                    <div className="invoice-product-combo" ref={searchRef}>
                      <input
                        type="text"
                        placeholder="Type to search products..."
                        value={productSearch}
                        onChange={(e) => {
                          setProductSearch(e.target.value);
                          setAddProductId(0);
                          setAddProductName('');
                          setShowProductDropdown(true);
                        }}
                        onFocus={() => setShowProductDropdown(true)}
                        className="invoice-product-search"
                      />
                      {addProductId > 0 && (
                        <div className="invoice-selected-product">
                          <span>{addProductName}</span>
                          <button onClick={() => { setAddProductId(0); setAddProductName(''); setProductSearch(''); }}>×</button>
                        </div>
                      )}
                      {showProductDropdown && productSearch && filteredProducts.length > 0 && !addProductId && (
                        <div className="invoice-product-dropdown">
                          {filteredProducts.slice(0, 10).map((p) => (
                            <div
                              key={p.id}
                              className="invoice-product-option"
                              onMouseDown={() => {
                                setAddProductId(p.id!);
                                setAddProductName(p.productName);
                                setProductSearch(p.productName);
                                setShowProductDropdown(false);
                              }}
                            >
                              <span className="opt-name">{p.productName}</span>
                              <span className="opt-meta">${Number(p.price).toFixed(2)} · Stock: {p.productQuantity}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                    <input
                      type="number"
                      min={1}
                      value={addQty}
                      onChange={(e) => setAddQty(e.target.value === '' ? 0 : parseInt(e.target.value))}
                      className="invoice-qty-input"
                      placeholder="Qty"
                    />
                    <button className="btn btn-accent" onClick={handleAddItem} disabled={!addProductId}>
                      Add
                    </button>
                  </div>
                </div>
              )}

              {/* Items Table */}
              <div ref={printRef}>
                <div className="inv-header" style={{ display: 'none' }}>
                  <div>
                    <div className="inv-brand">CFT Electronic</div>
                    <div className="inv-brand-sub">Inventory Management System</div>
                  </div>
                  <div>
                    <div className="inv-title">{activeInvoice.invoiceName || `Invoice #${activeInvoice.id}`}</div>
                    <div className="inv-meta">
                      <span>Date: {activeInvoice.createdAt ? new Date(activeInvoice.createdAt).toLocaleDateString() : '—'}</span>
                      {activeInvoice.clientName && <span>Client: {activeInvoice.clientName}</span>}
                      <span>Seller: {activeInvoice.sellerUsername}</span>
                    </div>
                  </div>
                </div>

                {activeInvoice.items && activeInvoice.items.length > 0 ? (
                  <table className="data-table invoice-table">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Unit Price</th>
                        <th>Qty</th>
                        <th>Line Total</th>
                        {activeInvoice.status === 'OPEN' && <th>Action</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {activeInvoice.items.map((item) => (
                        <tr key={item.id}>
                          <td>{item.productName}</td>
                          <td>${Number(item.unitPrice).toFixed(2)}</td>
                          <td>{item.quantity}</td>
                          <td>${Number(item.lineTotal).toFixed(2)}</td>
                          {activeInvoice.status === 'OPEN' && (
                            <td>
                              <button className="btn btn-sm btn-danger" onClick={() => handleRemoveItem(item.id!)}>
                                Remove
                              </button>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <p className="empty-state" style={{ padding: '1.5rem', margin: '1rem 0' }}>No items in this invoice yet.</p>
                )}

                {/* Totals */}
                <div className="invoice-totals">
                  <div className="invoice-totals-row">
                    <span>Subtotal:</span>
                    <span>${Number(activeInvoice.subtotal).toFixed(2)}</span>
                  </div>
                  {Number(activeInvoice.discountPercent) > 0 && (
                    <div className="invoice-totals-row discount">
                      <span>Discount ({Number(activeInvoice.discountPercent).toFixed(1)}%):</span>
                      <span>-${Number(activeInvoice.discountAmount).toFixed(2)}</span>
                    </div>
                  )}
                  <div className="invoice-totals-row tax">
                    <span>ITBMS ({Number(activeInvoice.taxPercent).toFixed(0)}%):</span>
                    <span>${Number(activeInvoice.taxAmount).toFixed(2)}</span>
                  </div>
                  <div className="invoice-totals-row grand-total">
                    <span>TOTAL:</span>
                    <span>${Number(activeInvoice.total).toFixed(2)}</span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="invoice-actions">
                {activeInvoice.status === 'OPEN' && (
                  <>
                    <button className="btn btn-secondary" onClick={() => openSettingsFor(activeInvoice)}>
                      Settings
                    </button>
                    <button className="btn btn-accent" onClick={handleFinalize} disabled={!activeInvoice.items?.length}>
                      Finalize Invoice
                    </button>
                    <button className="btn btn-danger" onClick={handleCancel}>
                      Cancel
                    </button>
                  </>
                )}
                <button className="btn btn-primary" onClick={handleExportPDF}>
                  Export PDF
                </button>
              </div>
            </>
          ) : (
            <div className="detail-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" width="48" height="48"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <p>Select or create an invoice</p>
            </div>
          )}
        </div>
      </div>

      {/* Settings Modal */}
      {showSettings && activeInvoice && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Invoice Settings</h2>
            <div className="form-group">
              <label>Invoice Name</label>
              <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} placeholder="e.g. Order for John" />
            </div>
            <div className="form-group">
              <label>Client Name</label>
              <input type="text" value={editClient} onChange={(e) => setEditClient(e.target.value)} placeholder="Client name" />
            </div>
            <div className="form-group">
              <label>Discount (%)</label>
              <input type="number" step="0.1" min="0" max="100" value={editDiscount} onChange={(e) => setEditDiscount(e.target.value)} />
            </div>
            <div className="form-actions">
              <button className="btn btn-secondary" onClick={() => setShowSettings(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSaveSettings}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
