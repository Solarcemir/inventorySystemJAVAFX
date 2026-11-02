import { useState } from 'react';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import Login from './pages/Login';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import AddProduct from './pages/AddProduct';
import Inventory from './pages/Inventory';
import Clients from './pages/Clients';
import Sales from './pages/Sales';
import Analytics from './pages/Analytics';
import Installations from './pages/Installations';
import Calendar from './pages/Calendar';
import ActivityLogPage from './pages/ActivityLogPage';
import Invoices from './pages/Invoices';
import { isAdmin } from './services/authService';

function AdminRoute() {
  return isAdmin() ? <Outlet /> : <Navigate to="/" replace />;
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(
    () => sessionStorage.getItem('authenticated') === 'true'
  );

  if (!authenticated) {
    return <Login onLogin={() => setAuthenticated(true)} />;
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/inventory" element={<Inventory />} />
        <Route path="/clients" element={<Clients />} />
        <Route path="/installations" element={<Installations />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/invoices" element={<Invoices />} />
        <Route element={<AdminRoute />}>
          <Route path="/add-product" element={<AddProduct />} />
          <Route path="/sales" element={<Sales />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/activity-log" element={<ActivityLogPage />} />
        </Route>
      </Route>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
