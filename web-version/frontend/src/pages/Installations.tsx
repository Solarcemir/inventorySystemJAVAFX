import { useEffect, useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { installationService } from '../services/installationService';
import { invoiceService } from '../services/invoiceService';
import { getCurrentUser } from '../services/authService';
import { Installation } from '../types';

// ── Persistent autocomplete suggestions ──────────────────────────────────────
function loadSuggestions(key: string): string[] {
  try { return JSON.parse(localStorage.getItem(`cft-sug-${key}`) || '[]'); } catch { return []; }
}
function saveSuggestion(key: string, value: string) {
  if (!value.trim()) return;
  const prev = loadSuggestions(key);
  const updated = [value, ...prev.filter((v) => v !== value)].slice(0, 20);
  localStorage.setItem(`cft-sug-${key}`, JSON.stringify(updated));
}
// ─────────────────────────────────────────────────────────────────────────────

const emptyInstallation: Installation = {
  serviceName: '',
  clientName: '',
  installerName: '',
  description: '',
  price: 0,
  status: 'PENDING',
};

export default function Installations() {
  const navigate = useNavigate();
  const { username } = getCurrentUser();
  const [installations, setInstallations] = useState<Installation[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Installation | null>(null);
  const [form, setForm] = useState<Installation>(emptyInstallation);
  const [error, setError] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterInstaller, setFilterInstaller] = useState('');
  const [creatingInvoiceFor, setCreatingInvoiceFor] = useState<number | null>(null);

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
      // Save autocomplete suggestions
      saveSuggestion('inst-service', form.serviceName);
      saveSuggestion('inst-client', form.clientName);
      saveSuggestion('inst-installer', form.installerName);
      setShowForm(false);
      setEditing(null);
      setForm(emptyInstallation);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save installation');
    }
  };

  const handleCreateInvoice = async (inst: Installation) => {
    setCreatingInvoiceFor(inst.id!);
    try {
      await invoiceService.create({
        sellerUsername: username,
        invoiceName: `Inst: ${inst.serviceName}`,
        clientName: inst.clientName || '',
      });
      navigate('/invoices');
    } catch (err: any) {
      const msg = err.response?.data;
      alert(typeof msg === 'string' ? msg : msg?.message || 'Failed to create invoice (max 5 open)');
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
    if (!window.confirm('Delete this installation record?')) return;
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

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'status-completed';
      case 'PENDING': return 'status-pending';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  };

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

      <div className="sales-toolbar">
        <div className="sales-filter-group">
          <label>Status:</label>
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
        <div className="sales-filter-group">
          <label>Installer:</label>
          <select value={filterInstaller} onChange={(e) => setFilterInstaller(e.target.value)}>
            <option value="">All Installers</option>
            {installers.map((ins) => <option key={ins} value={ins}>{ins}</option>)}
          </select>
        </div>
      </div>

      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Edit Installation' : 'New Installation'}</h2>
            <form onSubmit={handleSubmit}>
              {/* Autocomplete datalists */}
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
                  <label>Price</label>
                  <input type="number" step="0.01" min="0" value={form.price} onChange={(e) => setForm({ ...form, price: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="form-group">
                  <label>Status</label>
                  <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                    <option value="PENDING">Pending</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
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
              <th>Installer</th>
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
                <td>{inst.serviceName}</td>
                <td>{inst.clientName || '—'}</td>
                <td><strong>{inst.installerName}</strong></td>
                <td>${Number(inst.price).toFixed(2)}</td>
                <td><span className={`status-badge ${getStatusClass(inst.status)}`}>{inst.status}</span></td>
                <td>{inst.serviceDate ? new Date(inst.serviceDate).toLocaleDateString() : '—'}</td>
                <td>
                  <button className="btn btn-sm btn-primary" onClick={() => handleCreateInvoice(inst)} disabled={creatingInvoiceFor === inst.id}>
                    {creatingInvoiceFor === inst.id ? '…' : 'Invoice'}
                  </button>
                  <button className="btn btn-sm btn-secondary" onClick={() => handleEdit(inst)}>Edit</button>
                  <button className="btn btn-sm btn-danger" onClick={() => handleDelete(inst.id!)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
