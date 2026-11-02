import { useEffect, useState, useMemo, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { invoiceService } from '../services/invoiceService';
import { productService } from '../services/productService';
import { clientService } from '../services/clientService';
import { installationService } from '../services/installationService';
import { imageService } from '../services/imageService';
import { isAdmin } from '../services/authService';
import { getErrorMessage } from '../services/api';
import { Invoice, Product, Client, Installation } from '../types';

const emptyInstallation = (): Omit<Installation, 'id'> => ({
  serviceName: '',
  clientName: '',
  installerName: '',
  description: '',
  price: 0,
  serviceDate: '',
  status: 'PENDING',
  invoiceNumber: '',
  carModel: '',
  invoiceId: undefined,
});

export default function Invoices() {
  const admin = isAdmin();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeInvoice, setActiveInvoice] = useState<Invoice | null>(null);
  const [error, setError] = useState('');

  // History view toggle
  const [view, setView] = useState<'workspace' | 'history'>('workspace');

  // History filters
  const [hSearch, setHSearch] = useState('');
  const [hStatus, setHStatus] = useState('');
  const [hDateFrom, setHDateFrom] = useState('');
  const [hDateTo, setHDateTo] = useState('');

  // Add item state
  const [addProductId, setAddProductId] = useState(0);
  const [addProductName, setAddProductName] = useState('');
  const [addQty, setAddQty] = useState(1);
  const [addingItem, setAddingItem] = useState(false);
  const [creatingInvoice, setCreatingInvoice] = useState(false);
  const [productSearch, setProductSearch] = useState('');
  const [showProductDropdown, setShowProductDropdown] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // Settings modal state
  const [editName, setEditName] = useState('');
  const [editClient, setEditClient] = useState('');
  const [editDiscount, setEditDiscount] = useState('');
  const [editInvoiceNumber, setEditInvoiceNumber] = useState('');
  const [editCarModel, setEditCarModel] = useState('');
  const [showSettings, setShowSettings] = useState(false);

  // Delete confirmation modal state
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Installation modal state
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [installForm, setInstallForm] = useState<Omit<Installation, 'id'>>(emptyInstallation());
  const [savingInstall, setSavingInstall] = useState(false);

  // Linked installations for the active invoice
  const [linkedInstallations, setLinkedInstallations] = useState<Installation[]>([]);

  const printRef = useRef<HTMLDivElement>(null);

  // Set when arriving from the Installations page
  const openInvoiceId = (useLocation().state as { openInvoiceId?: number } | null)?.openInvoiceId;

  useEffect(() => {
    if (openInvoiceId) {
      invoiceService.getById(openInvoiceId)
        .then(setActiveInvoice)
        .catch((err) => setError(getErrorMessage(err, 'Failed to open invoice')));
    }
  }, [openInvoiceId]);

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

  useEffect(() => {
    if (activeInvoice?.id) {
      installationService.getByInvoiceId(activeInvoice.id)
        .then(setLinkedInstallations)
        .catch(() => setLinkedInstallations([]));
    } else {
      setLinkedInstallations([]);
    }
  }, [activeInvoice?.id]);

  const loadData = async () => {
    try {
      const [invs, prods, cls] = await Promise.all([invoiceService.getAll(), productService.getAll(), clientService.getAll()]);
      setInvoices(invs);
      setProducts(prods);
      setClients(cls);
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

  const historyFiltered = useMemo(() => {
    return invoices.filter((inv) => {
      if (hStatus && inv.status !== hStatus) return false;
      if (hSearch) {
        const q = hSearch.toLowerCase();
        const match =
          inv.invoiceName?.toLowerCase().includes(q) ||
          inv.clientName?.toLowerCase().includes(q) ||
          inv.invoiceNumber?.toLowerCase().includes(q) ||
          inv.carModel?.toLowerCase().includes(q) ||
          inv.sellerUsername?.toLowerCase().includes(q) ||
          String(inv.id).includes(q);
        if (!match) return false;
      }
      if (hDateFrom && inv.createdAt) {
        if (new Date(inv.createdAt) < new Date(hDateFrom)) return false;
      }
      if (hDateTo && inv.createdAt) {
        const to = new Date(hDateTo);
        to.setHours(23, 59, 59, 999);
        if (new Date(inv.createdAt) > to) return false;
      }
      return true;
    });
  }, [invoices, hStatus, hSearch, hDateFrom, hDateTo]);

  const filteredProducts = useMemo(() => {
    if (!productSearch) return products;
    return products.filter((p) => productService.matchesSearch(p, productSearch));
  }, [products, productSearch]);

  const handleCreate = async () => {
    setError('');
    if (openInvoices.length >= 5) {
      setError('Maximum 5 open invoices allowed. Finalize or cancel one first.');
      return;
    }
    setCreatingInvoice(true);
    try {
      const inv = await invoiceService.create({ invoiceName: `Invoice #${invoices.length + 1}` });
      await loadData();
      setActiveInvoice(inv);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to create invoice'));
    } finally {
      setCreatingInvoice(false);
    }
  };

  const handleAddItem = async () => {
    if (!activeInvoice || !addProductId) return;
    setError('');
    if (!Number.isInteger(addQty) || addQty < 1) {
      setError('Quantity must be at least 1');
      return;
    }
    setAddingItem(true);
    try {
      const updated = await invoiceService.addItem(activeInvoice.id!, addProductId, addQty);
      setActiveInvoice(updated);
      setAddProductId(0);
      setAddProductName('');
      setAddQty(1);
      setProductSearch('');
      setShowProductDropdown(false);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to add item'));
    } finally {
      setAddingItem(false);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (!activeInvoice) return;
    try {
      const updated = await invoiceService.removeItem(activeInvoice.id!, itemId);
      setActiveInvoice(updated);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to remove item'));
    }
  };

  const handleSaveSettings = async () => {
    if (!activeInvoice) return;
    try {
      const updated = await invoiceService.updateSettings(activeInvoice.id!, {
        invoiceName: editName,
        clientName: editClient,
        discountPercent: editDiscount || '0',
        invoiceNumber: editInvoiceNumber,
        carModel: editCarModel,
      });
      setActiveInvoice(updated);
      setShowSettings(false);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to update settings'));
      setShowSettings(false);
    }
  };

  const handleFinalize = async () => {
    if (!activeInvoice) return;
    if (!window.confirm('Finalize this invoice? This will deduct items from inventory and cannot be undone.')) return;
    try {
      const updated = await invoiceService.finalize(activeInvoice.id!);
      setActiveInvoice(updated);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to finalize'));
    }
  };

  const handleReopen = async () => {
    if (!activeInvoice) return;
    if (!window.confirm(
      'Reopen this invoice for editing?\n\n' +
      'This will:\n• Reverse inventory deductions\n• Delete the profit records for this invoice\n\n' +
      'You can then edit items and re-finalize.'
    )) return;
    try {
      const updated = await invoiceService.reopen(activeInvoice.id!);
      setActiveInvoice(updated);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to reopen invoice'));
    }
  };

  const handleDelete = async () => {
    if (!activeInvoice) return;
    setDeleting(true);
    try {
      await invoiceService.deleteInvoice(activeInvoice.id!);
      setActiveInvoice(null);
      setShowDeleteModal(false);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to delete invoice'));
      setShowDeleteModal(false);
    } finally {
      setDeleting(false);
    }
  };

  const handleCancel = async () => {
    if (!activeInvoice) return;
    if (!window.confirm('Cancel this invoice? Items will NOT be deducted from inventory.')) return;
    try {
      await invoiceService.cancel(activeInvoice.id!);
      setActiveInvoice(null);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to cancel invoice'));
    }
  };

  const openInstallModalFor = (inv: Invoice) => {
    setInstallForm({
      serviceName: '',
      clientName: inv.clientName || '',
      installerName: '',
      description: '',
      price: 0,
      serviceDate: '',
      status: 'PENDING',
      invoiceNumber: inv.invoiceNumber || '',
      carModel: inv.carModel || '',
      invoiceId: inv.id,
    });
    setShowInstallModal(true);
  };

  const handleSaveInstallation = async () => {
    if (!installForm.serviceName.trim()) {
      setError('Service name is required');
      return;
    }
    if (!installForm.installerName.trim()) {
      setError('Installer name is required');
      return;
    }
    setSavingInstall(true);
    try {
      await installationService.create(installForm as Installation);
      setShowInstallModal(false);
      setInstallForm(emptyInstallation());
      // Refresh linked installations panel
      if (activeInvoice?.id) {
        installationService.getByInvoiceId(activeInvoice.id).then(setLinkedInstallations).catch(() => {});
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save installation'));
    } finally {
      setSavingInstall(false);
    }
  };

  const handleExportPDF = () => {
    if (!printRef.current) return;
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;
    printWindow.document.write(`
      <html><head><title>Invoice</title>
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
    // Set apart from the markup above: the name is typed by users, so it must never be parsed as HTML
    printWindow.document.title = activeInvoice?.invoiceName || 'Invoice';
    printWindow.focus();
    setTimeout(() => { printWindow.print(); }, 400);
  };

  const openSettingsFor = (inv: Invoice) => {
    setActiveInvoice(inv);
    setEditName(inv.invoiceName || '');
    setEditClient(inv.clientName || '');
    setEditDiscount(String(inv.discountPercent || 0));
    setEditInvoiceNumber(inv.invoiceNumber || '');
    setEditCarModel(inv.carModel || '');
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
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.5rem' }}>
          <button
            className={`btn ${view === 'workspace' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setView('workspace')}
          >
            Workspace
          </button>
          <button
            className={`btn ${view === 'history' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setView('history')}
          >
            📋 History ({invoices.length})
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* ── HISTORY VIEW ─────────────────────────────────────────── */}
      {view === 'history' && (
        <div>
          {/* Filters */}
          <div className="sales-toolbar" style={{ flexWrap: 'wrap', gap: '0.6rem', marginBottom: '1rem' }}>
            <div className="sales-filter-group" style={{ flex: '1 1 220px' }}>
              <label>Search</label>
              <input
                type="text"
                value={hSearch}
                onChange={(e) => setHSearch(e.target.value)}
                placeholder="Invoice name, client, number, car..."
                style={{ width: '100%' }}
              />
            </div>
            <div className="sales-filter-group">
              <label>Status</label>
              <select value={hStatus} onChange={(e) => setHStatus(e.target.value)}>
                <option value="">All</option>
                <option value="OPEN">Open</option>
                <option value="FINALIZED">Finalized</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
            <div className="sales-filter-group">
              <label>Date From</label>
              <input type="date" value={hDateFrom} onChange={(e) => setHDateFrom(e.target.value)} />
            </div>
            <div className="sales-filter-group">
              <label>Date To</label>
              <input type="date" value={hDateTo} onChange={(e) => setHDateTo(e.target.value)} />
            </div>
            {(hSearch || hStatus || hDateFrom || hDateTo) && (
              <div className="sales-filter-group" style={{ alignSelf: 'flex-end' }}>
                <button className="btn btn-sm btn-secondary" onClick={() => { setHSearch(''); setHStatus(''); setHDateFrom(''); setHDateTo(''); }}>
                  Clear
                </button>
              </div>
            )}
          </div>

          {/* Summary */}
          {(() => {
            const finalized = historyFiltered.filter(i => i.status === 'FINALIZED');
            const totalRev = finalized.reduce((s, i) => s + Number(i.total || 0), 0);
            return (
              <div style={{ display: 'flex', gap: '1.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                <span>Showing <strong>{historyFiltered.length}</strong> of {invoices.length}</span>
                <span>·</span>
                <span>Finalized: <strong>{finalized.length}</strong></span>
                <span>·</span>
                <span>Total invoiced (incl. ITBMS): <strong style={{ color: 'var(--accent)' }}>${totalRev.toFixed(2)}</strong></span>
              </div>
            );
          })()}

          {/* Table */}
          {historyFiltered.length === 0 ? (
            <p className="empty-state">No invoices match the filters.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Invoice #</th>
                  <th>Name</th>
                  <th>Client</th>
                  <th>Car</th>
                  <th>Seller</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {historyFiltered.map((inv) => (
                  <tr key={inv.id} style={{ cursor: 'pointer' }}>
                    <td>{inv.id}</td>
                    <td>{inv.invoiceNumber ? <span style={{ color: 'var(--primary)', fontWeight: 500 }}>#{inv.invoiceNumber}</span> : '—'}</td>
                    <td>{inv.invoiceName || `Invoice #${inv.id}`}</td>
                    <td>{inv.clientName || '—'}</td>
                    <td>{inv.carModel ? <span>🚗 {inv.carModel}</span> : '—'}</td>
                    <td>{inv.sellerUsername}</td>
                    <td>{inv.items?.length ?? 0}</td>
                    <td style={{ fontWeight: 600 }}>${Number(inv.total).toFixed(2)}</td>
                    <td>
                      <span className={`status-badge ${inv.status === 'OPEN' ? 'status-pending' : inv.status === 'FINALIZED' ? 'status-completed' : 'status-cancelled'}`}>
                        {inv.status}
                      </span>
                    </td>
                    <td>{inv.createdAt ? new Date(inv.createdAt).toLocaleDateString() : '—'}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => { setView('workspace'); setActiveInvoice(inv); }}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* ── WORKSPACE VIEW ───────────────────────────────────────── */}
      {view === 'workspace' && <div className="invoice-layout">
        {/* Left: Invoice List + Create */}
        <div className="invoice-list-panel">
          <div className="invoice-list-header">
            <h2>Open Invoices ({openInvoices.length}/5)</h2>
            <button className="btn btn-primary" onClick={handleCreate} disabled={openInvoices.length >= 5 || creatingInvoice}>
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
                  {inv.invoiceNumber && <div className="invoice-card-client" style={{ color: 'var(--primary)' }}>#{inv.invoiceNumber}</div>}
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
                    {inv.invoiceNumber && <div className="invoice-card-client" style={{ color: 'var(--primary)' }}>#{inv.invoiceNumber}</div>}
                    {inv.carModel && <div className="invoice-card-client">🚗 {inv.carModel}</div>}
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
                    {activeInvoice.invoiceNumber && <><strong>#{activeInvoice.invoiceNumber}</strong> · </>}
                    {activeInvoice.clientName && `Client: ${activeInvoice.clientName} · `}
                    {activeInvoice.carModel && `🚗 ${activeInvoice.carModel} · `}
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
                      {addProductId > 0 && (() => {
                        const selProd = products.find(p => p.id === addProductId);
                        const src = imageService.toSrc(selProd?.imagePath);
                        return (
                          <div className="invoice-selected-product">
                            {src && <img src={src} alt="" className="selected-prod-thumb" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />}
                            <span>{addProductName}</span>
                            {src && <a href={src} target="_blank" rel="noreferrer" className="selected-prod-view" title="View full image">🔍</a>}
                            <button onClick={() => { setAddProductId(0); setAddProductName(''); setProductSearch(''); }}>×</button>
                          </div>
                        );
                      })()}
                      {showProductDropdown && productSearch && filteredProducts.length > 0 && !addProductId && (
                        <div className="invoice-product-dropdown">
                          {filteredProducts.slice(0, 10).map((p) => {
                            const src = imageService.toSrc(p.imagePath);
                            return (
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
                                {src
                                  ? <img src={src} alt="" className="opt-thumb" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                                  : <div className="opt-thumb-placeholder" />
                                }
                                <div className="opt-info">
                                  <span className="opt-name">{p.productName}</span>
                                  <span className="opt-meta">${Number(p.price).toFixed(2)} · Stock: {p.productQuantity}</span>
                                </div>
                              </div>
                            );
                          })}
                          {filteredProducts.length > 10 && (
                            <div style={{ padding: '0.5rem 0.75rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                              Showing 10 of {filteredProducts.length}. Keep typing to narrow it down.
                            </div>
                          )}
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
                    <button className="btn btn-accent" onClick={handleAddItem} disabled={!addProductId || addQty < 1 || addingItem}>
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
                      {activeInvoice.invoiceNumber && <span>Invoice #: {activeInvoice.invoiceNumber}</span>}
                      <span>Date: {activeInvoice.createdAt ? new Date(activeInvoice.createdAt).toLocaleDateString() : '—'}</span>
                      {activeInvoice.clientName && <span>Client: {activeInvoice.clientName}</span>}
                      {activeInvoice.carModel && <span>Car: {activeInvoice.carModel}</span>}
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
                      {activeInvoice.items.map((item) => {
                        const prod = products.find(p => p.id === item.productId);
                        const src = imageService.toSrc(prod?.imagePath);
                        return (
                          <tr key={item.id}>
                            <td>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                {src
                                  ? <img src={src} alt="" style={{ width: 32, height: 32, objectFit: 'cover', borderRadius: 4 }} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                                  : <div style={{ width: 32, height: 32, background: 'var(--bg)', borderRadius: 4, flexShrink: 0 }} />
                                }
                                <span>{item.productName}</span>
                                {src && <a href={src} target="_blank" rel="noreferrer" title="View image" style={{ fontSize: '0.8rem' }}>🔍</a>}
                              </div>
                            </td>
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
                        );
                      })}
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

              {/* Linked Installations */}
              {linkedInstallations.length > 0 && (
                <div style={{ marginTop: '1.25rem' }}>
                  <h3 style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.6rem', color: 'var(--text)' }}>
                    🔧 Linked Installations ({linkedInstallations.length})
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {linkedInstallations.map((inst) => (
                      <div key={inst.id} style={{
                        background: 'var(--bg)',
                        border: '1px solid var(--border)',
                        borderRadius: 'var(--radius)',
                        padding: '0.65rem 0.9rem',
                        display: 'grid',
                        gridTemplateColumns: '1fr auto',
                        gap: '0.25rem 1rem',
                        alignItems: 'start',
                      }}>
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{inst.serviceName}</div>
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.15rem' }}>
                            {inst.installerName && <span>Installer: <strong>{inst.installerName}</strong></span>}
                            {inst.clientName && <span style={{ marginLeft: '0.75rem' }}>Client: {inst.clientName}</span>}
                            {inst.carModel && <span style={{ marginLeft: '0.75rem' }}>🚗 {inst.carModel}</span>}
                          </div>
                          {inst.description && (
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.1rem' }}>{inst.description}</div>
                          )}
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <span className={`status-badge ${inst.status === 'COMPLETED' ? 'status-completed' : inst.status === 'CANCELLED' ? 'status-cancelled' : 'status-pending'}`} style={{ fontSize: '0.7rem' }}>
                            {inst.status}
                          </span>
                          {inst.price != null && Number(inst.price) > 0 && (
                            <div style={{ fontSize: '0.85rem', fontWeight: 600, marginTop: '0.25rem' }}>${Number(inst.price).toFixed(2)}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="invoice-actions">
                {(admin || activeInvoice.status === 'OPEN') && (
                  <button className="btn btn-secondary" onClick={() => openSettingsFor(activeInvoice)}>
                    Settings
                  </button>
                )}

                {activeInvoice.status === 'OPEN' && (
                  <>
                    <button className="btn btn-accent" onClick={handleFinalize} disabled={!activeInvoice.items?.length}>
                      Finalize Invoice
                    </button>
                    <button className="btn btn-danger" onClick={handleCancel}>
                      Cancel
                    </button>
                  </>
                )}

                {activeInvoice.status === 'FINALIZED' && (
                  <>
                    {admin && (
                      <button className="btn btn-secondary" onClick={handleReopen} title="Reverse inventory and re-open for editing">
                        ✏️ Edit (Reopen)
                      </button>
                    )}
                    <button className="btn" style={{ background: '#8e44ad', color: '#fff' }} onClick={() => openInstallModalFor(activeInvoice)}>
                      🔧 Assign Installation
                    </button>
                    {admin && (
                      <button className="btn btn-danger" onClick={() => setShowDeleteModal(true)}>
                        🗑️ Delete
                      </button>
                    )}
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
      </div>}

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
              <label>Invoice Number</label>
              <input type="text" value={editInvoiceNumber} onChange={(e) => setEditInvoiceNumber(e.target.value)} placeholder="e.g. FAC-2024-001" />
            </div>
            <div className="form-group">
              <label>Client Name</label>
              <input list="invoice-clients" type="text" value={editClient} onChange={(e) => setEditClient(e.target.value)} placeholder="Client name" />
              <datalist id="invoice-clients">
                {clients.map((c) => <option key={c.id} value={`${c.firstName} ${c.lastName}`} />)}
              </datalist>
              <small style={{ color: 'var(--text-secondary)' }}>A registered client's name adds this invoice to their Total Spent.</small>
            </div>
            <div className="form-group">
              <label>Car Model</label>
              <input type="text" value={editCarModel} onChange={(e) => setEditCarModel(e.target.value)} placeholder="e.g. Toyota Corolla 2020" />
            </div>
            {admin && activeInvoice.status === 'OPEN' && (
              <div className="form-group">
                <label>Discount (%)</label>
                <input type="number" step="0.1" min="0" max="100" value={editDiscount} onChange={(e) => setEditDiscount(e.target.value)} />
              </div>
            )}
            <div className="form-actions">
              <button className="btn btn-secondary" onClick={() => setShowSettings(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSaveSettings}>Save</button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && activeInvoice && (
        <div className="modal-overlay" onClick={() => !deleting && setShowDeleteModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2 style={{ color: 'var(--danger)' }}>⚠️ Delete Invoice</h2>
            <p style={{ marginBottom: '1rem' }}>
              You are about to permanently delete <strong>{activeInvoice.invoiceName || `Invoice #${activeInvoice.id}`}</strong>.
            </p>
            {activeInvoice.status === 'FINALIZED' && (
              <div style={{ background: '#fff3cd', border: '1px solid #ffc107', borderRadius: 'var(--radius)', padding: '0.75rem 1rem', marginBottom: '1rem', fontSize: '0.9rem' }}>
                <strong>This invoice was finalized. Deleting it will:</strong>
                <ul style={{ marginTop: '0.5rem', paddingLeft: '1.2rem' }}>
                  <li>Remove all profit/sales records from analytics</li>
                  <li>Restore product quantities back to inventory</li>
                </ul>
              </div>
            )}
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>This action cannot be undone.</p>
            <div className="form-actions" style={{ marginTop: '1.25rem' }}>
              <button className="btn btn-secondary" onClick={() => setShowDeleteModal(false)} disabled={deleting}>Cancel</button>
              <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                {deleting ? 'Deleting...' : 'Yes, Delete Invoice'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Assign Installation Modal */}
      {showInstallModal && (
        <div className="modal-overlay" onClick={() => !savingInstall && setShowInstallModal(false)}>
          <div className="modal" style={{ maxWidth: 520 }} onClick={(e) => e.stopPropagation()}>
            <h2>🔧 Assign Installation</h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              This will create an installation record linked to invoice #{activeInvoice?.invoiceNumber || activeInvoice?.id}.
            </p>
            <div className="form-group">
              <label>Service Name *</label>
              <input type="text" value={installForm.serviceName} onChange={(e) => setInstallForm(f => ({ ...f, serviceName: e.target.value }))} placeholder="e.g. Car Audio Installation" />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Client Name</label>
                <input type="text" value={installForm.clientName} onChange={(e) => setInstallForm(f => ({ ...f, clientName: e.target.value }))} placeholder="Client name" />
              </div>
              <div className="form-group">
                <label>Car Model</label>
                <input type="text" value={installForm.carModel || ''} onChange={(e) => setInstallForm(f => ({ ...f, carModel: e.target.value }))} placeholder="e.g. Honda Civic 2019" />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Installer Name *</label>
                <input type="text" value={installForm.installerName} onChange={(e) => setInstallForm(f => ({ ...f, installerName: e.target.value }))} placeholder="Technician name" />
              </div>
              <div className="form-group">
                <label>Invoice Number</label>
                <input type="text" value={installForm.invoiceNumber || ''} onChange={(e) => setInstallForm(f => ({ ...f, invoiceNumber: e.target.value }))} placeholder="e.g. FAC-2024-001" />
              </div>
            </div>
            <div className="form-group">
              <label>Description</label>
              <input type="text" value={installForm.description || ''} onChange={(e) => setInstallForm(f => ({ ...f, description: e.target.value }))} placeholder="Details of the installation" />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Price ($)</label>
                <input type="number" step="0.01" min="0" value={installForm.price} onChange={(e) => setInstallForm(f => ({ ...f, price: parseFloat(e.target.value) || 0 }))} />
              </div>
              <div className="form-group">
                <label>Service Date</label>
                <input type="datetime-local" value={installForm.serviceDate || ''} onChange={(e) => setInstallForm(f => ({ ...f, serviceDate: e.target.value }))} />
              </div>
            </div>
            <div className="form-group">
              <label>Status</label>
              <select value={installForm.status} onChange={(e) => setInstallForm(f => ({ ...f, status: e.target.value }))}>
                <option value="PENDING">Pending</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
            <div className="form-actions">
              <button className="btn btn-secondary" onClick={() => setShowInstallModal(false)} disabled={savingInstall}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSaveInstallation} disabled={savingInstall}>
                {savingInstall ? 'Saving...' : 'Save Installation'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
