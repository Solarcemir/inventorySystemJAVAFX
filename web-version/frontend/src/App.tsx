import { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
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
        <Route path="/add-product" element={<AddProduct />} />
        <Route path="/inventory" element={<Inventory />} />
        <Route path="/clients" element={<Clients />} />
        <Route path="/sales" element={<Sales />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/installations" element={<Installations />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/activity-log" element={<ActivityLogPage />} />
        <Route path="/invoices" element={<Invoices />} />
      </Route>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
