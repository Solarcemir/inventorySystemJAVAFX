import { useEffect, useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { installationService } from '../services/installationService';
import { isAdmin } from '../services/authService';
import { getErrorMessage } from '../services/api';
import { Installation } from '../types';

function loadSuggestions(key: string): string[] {
  try { return JSON.parse(localStorage.getItem(`cft-sug-${key}`) || '[]'); } catch { return []; }
}
function saveSuggestion(key: string, value: string) {
  if (!value.trim()) return;
  const prev = loadSuggestions(key);
  const updated = [value, ...prev.filter((v) => v !== value)].slice(0, 20);
  localStorage.setItem(`cft-sug-${key}`, JSON.stringify(updated));
}

const emptyInstallation: Installation = {
  serviceName: '',
  clientName: '',
  installerName: '',
  description: '',
  price: 0,
  status: 'PENDING',
  carModel: '',
  invoiceNumber: '',
};

export default function Installations() {
  const admin = isAdmin();
  const navigate = useNavigate();
  const [installations, setInstallations] = useState<Installation[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Installation | null>(null);
  const [form, setForm] = useState<Installation>(emptyInstallation);
  const [error, setError] = useState('');
  const [creatingInvoiceFor, setCreatingInvoiceFor] = useState<number | null>(null);

  // Filters
  const [filterStatus, setFilterStatus] = useState('');
  const [filterInstaller, setFilterInstaller] = useState('');
  const [filterSearch, setFilterSearch] = useState('');
  const [filterDateFrom, setFilterDateFrom] = useState('');
  const [filterDateTo, setFilterDateTo] = useState('');

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      setInstallations(await installationService.getAll());
    } catch (err) {
      console.error('Failed to load installations', err);
    } finally {
      setLoading(false);
    }
  };

  const installers = [...new Set(installations.map((i) => i.installerName).filter(Boolean))].sort();

  const filtered = installations.filter((i) => {
    if (filterStatus && i.status !== filterStatus) return false;
    if (filterInstaller && i.installerName !== filterInstaller) return false;
    if (filterSearch) {
      const q = filterSearch.toLowerCase();
      const match =
        i.serviceName?.toLowerCase().includes(q) ||
        i.clientName?.toLowerCase().includes(q) ||
        i.installerName?.toLowerCase().includes(q) ||
        i.description?.toLowerCase().includes(q) ||
        i.carModel?.toLowerCase().includes(q) ||
        i.invoiceNumber?.toLowerCase().includes(q);
      if (!match) return false;
    }
    if (filterDateFrom && i.serviceDate) {
      if (new Date(i.serviceDate) < new Date(filterDateFrom)) return false;
    }
    if (filterDateTo && i.serviceDate) {
      const to = new Date(filterDateTo);
      to.setHours(23, 59, 59, 999);
      if (new Date(i.serviceDate) > to) return false;
    }
    return true;
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.serviceName.trim()) { setError('Service name is required'); return; }
    if (!form.installerName.trim()) { setError('Installer name is required'); return; }
    try {
      if (editing) {
        await installationService.update(editing.id!, form);
      } else {
        await installationService.create(form);
      }
      saveSuggestion('inst-service', form.serviceName);
      saveSuggestion('inst-client', form.clientName);
      saveSuggestion('inst-installer', form.installerName);
      setShowForm(false);
      setEditing(null);
      setForm(emptyInstallation);
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save installation'));
    }
  };

  const handleCreateInvoice = async (inst: Installation) => {
    setCreatingInvoiceFor(inst.id!);
    try {
      const invoice = await installationService.createInvoice(inst.id!);
      navigate('/invoices', { state: { openInvoiceId: invoice.id } });
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to create invoice'));
    } finally {
      setCreatingInvoiceFor(null);
    }
  };

  const handleEdit = (inst: Installation) => {
    setForm({ ...inst });
    setEditing(inst);
    setShowForm(true);
    setError('');
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this installation record? This will also remove its revenue from analytics.')) return;
    try {
      await installationService.delete(id);
      await loadData();
    } catch (err) {
      console.error('Failed to delete', err);
    }
  };

  const openNewForm = () => {
    setForm(emptyInstallation);
    setEditing(null);
    setShowForm(true);
    setError('');
  };

  const clearFilters = () => {
    setFilterStatus('');
    setFilterInstaller('');
    setFilterSearch('');
    setFilterDateFrom('');
    setFilterDateTo('');
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'status-completed';
      case 'PENDING': return 'status-pending';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  };

  const totalRevenue = filtered
    .filter(i => i.status === 'COMPLETED')
    .reduce((sum, i) => sum + Number(i.price || 0), 0);

  if (loading) return <div className="page-loading">Loading installations...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title-row">
          <div className="page-title-icon install-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>
          </div>
          <div>
            <h1>Installation Services</h1>
            <p className="page-subtitle">Track installations and who performed them</p>
          </div>
        </div>
        <button className="btn btn-primary" onClick={openNewForm}>+ New Installation</button>
      </div>

      {/* Filter bar */}
      <div className="sales-toolbar" style={{ flexWrap: 'wrap', gap: '0.6rem' }}>
        <div className="sales-filter-group" style={{ flex: '1 1 200px' }}>
          <label>Search</label>
          <input
            type="text"
            value={filterSearch}
            onChange={(e) => setFilterSearch(e.target.value)}
            placeholder="Service, client, installer, car model..."
            style={{ width: '100%' }}
          />
        </div>
        <div className="sales-filter-group">
          <label>Status</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
        <div className="sales-filter-group">
          <label>Installer</label>
          <select value={filterInstaller} onChange={(e) => setFilterInstaller(e.target.value)}>
            <option value="">All Installers</option>
            {installers.map((ins) => <option key={ins} value={ins}>{ins}</option>)}
          </select>
        </div>
        <div className="sales-filter-group">
          <label>Date From</label>
          <input type="date" value={filterDateFrom} onChange={(e) => setFilterDateFrom(e.target.value)} />
        </div>
        <div className="sales-filter-group">
          <label>Date To</label>
          <input type="date" value={filterDateTo} onChange={(e) => setFilterDateTo(e.target.value)} />
        </div>
        {(filterStatus || filterInstaller || filterSearch || filterDateFrom || filterDateTo) && (
          <div className="sales-filter-group" style={{ alignSelf: 'flex-end' }}>
            <button className="btn btn-sm btn-secondary" onClick={clearFilters}>Clear</button>
          </div>
        )}
      </div>

      {/* Summary row */}
      <div style={{ display: 'flex', gap: '1rem', margin: '0.75rem 0', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
        <span>Showing <strong>{filtered.length}</strong> of {installations.length} records</span>
        <span>·</span>
        <span>Completed revenue: <strong style={{ color: 'var(--accent)' }}>${totalRevenue.toFixed(2)}</strong></span>
      </div>

      {/* Form modal */}
      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Edit Installation' : 'New Installation'}</h2>
            <form onSubmit={handleSubmit}>
              <datalist id="sug-service">{loadSuggestions('inst-service').map((s) => <option key={s} value={s} />)}</datalist>
              <datalist id="sug-client">{loadSuggestions('inst-client').map((s) => <option key={s} value={s} />)}</datalist>
              <datalist id="sug-installer">{loadSuggestions('inst-installer').map((s) => <option key={s} value={s} />)}</datalist>

              <div className="form-grid">
                <div className="form-group">
                  <label>Service Name *</label>
                  <input list="sug-service" type="text" value={form.serviceName} onChange={(e) => setForm({ ...form, serviceName: e.target.value })} placeholder="e.g. Radio Installation" required />
                </div>
                <div className="form-group">
                  <label>Client Name</label>
                  <input list="sug-client" type="text" value={form.clientName} onChange={(e) => setForm({ ...form, clientName: e.target.value })} placeholder="Client name" />
                </div>
                <div className="form-group">
                  <label>Installer Name *</label>
                  <input list="sug-installer" type="text" value={form.installerName} onChange={(e) => setForm({ ...form, installerName: e.target.value })} placeholder="Who performed the installation" required />
                </div>
                <div className="form-group">
                  <label>Price ($)</label>
                  <input type="number" step="0.01" min="0" value={form.price} disabled={!!editing && !admin} onChange={(e) => setForm({ ...form, price: parseFloat(e.target.value) || 0 })} />
                  {editing && !admin && <small style={{ color: 'var(--text-secondary)' }}>Only an admin can change the price.</small>}
                </div>
                <div className="form-group">
                  <label>Car Model</label>
                  <input type="text" value={form.carModel || ''} onChange={(e) => setForm({ ...form, carModel: e.target.value })} placeholder="e.g. Honda Civic 2020" />
                </div>
                <div className="form-group">
                  <label>Invoice Number</label>
                  <input type="text" value={form.invoiceNumber || ''} onChange={(e) => setForm({ ...form, invoiceNumber: e.target.value })} placeholder="e.g. FAC-2024-001" />
                </div>
                <div className="form-group">
                  <label>Status</label>
                  <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                    <option value="PENDING">Pending</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Service Date</label>
                  <input type="datetime-local" value={form.serviceDate ? form.serviceDate.substring(0, 16) : ''} onChange={(e) => setForm({ ...form, serviceDate: e.target.value })} />
                </div>
                <div className="form-group full-width">
                  <label>Description</label>
                  <textarea value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} placeholder="Details about the installation..." />
                </div>
              </div>
              {error && <div className="error-message">{error}</div>}
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editing ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <p className="empty-state">No installation records found.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Service</th>
              <th>Client</th>
              <th>Car Model</th>
              <th>Installer</th>
              <th>Invoice #</th>
              <th>Price</th>
              <th>Status</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((inst) => (
              <tr key={inst.id}>
                <td>{inst.id}</td>
                <td>
                  <div style={{ fontWeight: 500 }}>{inst.serviceName}</div>
                  {inst.description && <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{inst.description}</div>}
                </td>
                <td>{inst.clientName || '—'}</td>
                <td>{inst.carModel ? <span>🚗 {inst.carModel}</span> : '—'}</td>
                <td><strong>{inst.installerName}</strong></td>
                <td>{inst.invoiceNumber ? <span style={{ color: 'var(--primary)', fontWeight: 500 }}>#{inst.invoiceNumber}</span> : '—'}</td>
                <td style={{ fontWeight: 600 }}>${Number(inst.price).toFixed(2)}</td>
                <td><span className={`status-badge ${getStatusClass(inst.status)}`}>{inst.status}</span></td>
                <td>{inst.serviceDate ? new Date(inst.serviceDate).toLocaleDateString() : '—'}</td>
                <td>
                  <button className="btn btn-sm btn-primary" onClick={() => handleCreateInvoice(inst)} disabled={creatingInvoiceFor === inst.id}>
                    {creatingInvoiceFor === inst.id ? '…' : inst.invoiceId ? 'View Invoice' : 'Invoice'}
                  </button>
                  {(admin || inst.status !== 'COMPLETED') && (
                    <button className="btn btn-sm btn-secondary" onClick={() => handleEdit(inst)}>Edit</button>
                  )}
                  {admin && <button className="btn btn-sm btn-danger" onClick={() => handleDelete(inst.id!)}>Delete</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
