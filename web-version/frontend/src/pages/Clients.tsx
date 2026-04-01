import { useEffect, useState, FormEvent } from 'react';
import { clientService } from '../services/clientService';
import { logService } from '../services/logService';
import { isAdmin } from '../services/authService';
import { Client } from '../types';

const emptyClient: Client = { firstName: '', lastName: '', email: '', phoneNumber: '' };

export default function Clients() {
  const admin = isAdmin();
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<Client>(emptyClient);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => { loadClients(); }, []);

  const loadClients = async () => {
    try {
      setClients(await clientService.getAll());
    } catch (err) {
      console.error('Failed to load clients', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.firstName.trim() || !form.lastName.trim()) { setError('First and last name are required'); return; }
    try {
      await clientService.create(form);
      await logService.log('ADD_CLIENT', `Added client "${form.firstName} ${form.lastName}"`);
      setShowForm(false);
      setForm(emptyClient);
      await loadClients();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add client');
    }
  };

  const filtered = clients.filter((c) =>
    c.firstName.toLowerCase().includes(search.toLowerCase()) ||
    c.lastName.toLowerCase().includes(search.toLowerCase()) ||
    c.email.toLowerCase().includes(search.toLowerCase()) ||
    c.phoneNumber.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <div className="page-loading">Loading clients...</div>;

  return (
    <div className="page">
      <div className="page-title-row">
        <div className="page-title-icon cli">CLI</div>
        <div>
          <h1>Clients</h1>
          <p className="page-subtitle">Clients Management System</p>
        </div>
      </div>

      <div className="clients-toolbar">
        <input type="text" className="search-input" placeholder="Search clients..." value={search} onChange={(e) => setSearch(e.target.value)} />
        <button className="btn btn-accent" onClick={() => { setForm(emptyClient); setShowForm(true); setError(''); }}>Add Client</button>
      </div>

      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Add New Client</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label>First Name *</label>
                  <input type="text" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Last Name *</label>
                  <input type="text" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Email</label>
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Phone Number</label>
                  <input type="text" value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
                </div>
              </div>
              {error && <div className="error-message">{error}</div>}
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Client</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {filtered.length === 0 ? (
        <p className="empty-state">No clients found</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Phone Number</th>
              {admin && <th>Total Spent</th>}
            </tr>
          </thead>
          <tbody>
            {filtered.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.firstName} {c.lastName}</td>
                <td>{c.email}</td>
                <td>{c.phoneNumber}</td>
                {admin && <td>${Number(c.spentAmount || 0).toFixed(2)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
