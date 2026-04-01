import { useState, FormEvent } from 'react';
import { authService } from '../services/authService';
import { logService } from '../services/logService';

interface LoginProps {
  onLogin: () => void;
}

export default function Login({ onLogin }: LoginProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const result = await authService.login(username, password);
      if (result.success) {
        sessionStorage.setItem('authenticated', 'true');
        sessionStorage.setItem('username', result.username);
        sessionStorage.setItem('role', result.role);
        sessionStorage.setItem('userId', String(result.id));
        await logService.log('LOGIN', `User "${result.username}" logged in (${result.role})`);
        onLogin();
      } else {
        setError('Invalid username or password');
      }
    } catch {
      setError('Connection error. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-left">
        <div className="login-brand">
          <div className="login-logo">CFT</div>
          <h1>Inventory Management System</h1>
          <p>Manage your products, clients, and sales — all in one place.</p>
        </div>
      </div>
      <div className="login-right">
        <div className="login-card">
          <h2>Sign In</h2>
          <p className="login-subtitle">Enter your credentials to continue</p>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input id="username" type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter username" required autoFocus />
            </div>
            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter password" required />
            </div>
            {error && <div className="error-message">{error}</div>}
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Signing in...' : 'LOGIN'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
