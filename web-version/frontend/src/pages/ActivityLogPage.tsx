import { useEffect, useState, useMemo } from 'react';
import { logService } from '../services/logService';
import { ActivityLog } from '../types';

const ACTION_COLORS: Record<string, string> = {
  SALE: '#27ae60', ADD_PRODUCT: '#2980b9', DELETE_PRODUCT: '#e74c3c', EDIT_PRODUCT: '#f39c12',
  ADD_CLIENT: '#8e44ad', EDIT_CLIENT: '#f39c12', DELETE_CLIENT: '#e74c3c', DELETE_SALE: '#c0392b',
  INSTALLATION: '#e67e22', LOGIN: '#1abc9c', INVOICE: '#3498db',
};

export default function ActivityLogPage() {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterAction, setFilterAction] = useState('');
  const [filterUser, setFilterUser] = useState('');

  useEffect(() => { loadLogs(); }, []);

  const loadLogs = async () => {
    try { setLogs(await logService.getAll()); } catch (err) { console.error(err); } finally { setLoading(false); }
  };

  const actions = useMemo(() => [...new Set(logs.map((l) => l.action))].sort(), [logs]);
  const users = useMemo(() => [...new Set(logs.map((l) => l.username).filter(Boolean))].sort(), [logs]);

  const filtered = useMemo(() => {
    return logs.filter((l) => {
      if (filterAction && l.action !== filterAction) return false;
      if (filterUser && l.username !== filterUser) return false;
      return true;
    });
  }, [logs, filterAction, filterUser]);

  if (loading) return <div className="page-loading">Loading activity log...</div>;

  return (
    <div className="page">
      <div className="page-title-row">
        <div className="page-title-icon" style={{ background: '#34495e' }}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20"><polyline points="12 8 12 12 14 14"/><circle cx="12" cy="12" r="10"/></svg>
        </div>
        <div>
          <h1>Activity Log</h1>
          <p className="page-subtitle">Track all important actions</p>
        </div>
      </div>

      <div className="sales-toolbar">
        <div className="sales-filter-group">
          <label>Action:</label>
          <select value={filterAction} onChange={(e) => setFilterAction(e.target.value)}>
            <option value="">All Actions</option>
            {actions.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </div>
        <div className="sales-filter-group">
          <label>User:</label>
          <select value={filterUser} onChange={(e) => setFilterUser(e.target.value)}>
            <option value="">All Users</option>
            {users.map((u) => <option key={u} value={u}>{u}</option>)}
          </select>
        </div>
        <div className="sales-summary">
          <span>Total: <strong>{filtered.length}</strong> entries</span>
        </div>
      </div>

      {filtered.length === 0 ? (
        <p className="empty-state">No activity recorded yet.</p>
      ) : (
        <div className="log-list">
          {filtered.map((log) => (
            <div key={log.id} className="log-entry">
              <div className="log-action-badge" style={{ background: ACTION_COLORS[log.action] || '#7f8c8d' }}>
                {log.action}
              </div>
              <div className="log-body">
                <div className="log-desc">{log.description}</div>
                <div className="log-meta">
                  <span>{log.username}</span>
                  <span>{new Date(log.timestamp).toLocaleString()}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
