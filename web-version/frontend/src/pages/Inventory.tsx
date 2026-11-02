import { useEffect, useState, useMemo, FormEvent } from 'react';
import { productService } from '../services/productService';
import { clientService } from '../services/clientService';
import { saleService } from '../services/saleService';
import { imageService } from '../services/imageService';
import { getErrorMessage } from '../services/api';
import { isAdmin } from '../services/authService';
import { Product, Client } from '../types';

export default function Inventory() {
  const admin = isAdmin();
  const [products, setProducts] = useState<Product[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Product | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [filterCategory, setFilterCategory] = useState('');
  const [filterProvider, setFilterProvider] = useState('');
  const [filterLocation, setFilterLocation] = useState('');
  const [filterQtyMin, setFilterQtyMin] = useState('');
  const [filterQtyMax, setFilterQtyMax] = useState('');
  const [filterPriceMin, setFilterPriceMin] = useState('');
  const [filterPriceMax, setFilterPriceMax] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  // Sell dialog
  const [showSell, setShowSell] = useState(false);
  const [sellQty, setSellQty] = useState(1);
  const [sellClientId, setSellClientId] = useState(0);
  const [sellError, setSellError] = useState('');

  // Edit dialog
  const [showEdit, setShowEdit] = useState(false);
  const [editForm, setEditForm] = useState<Product | null>(null);
  const [editError, setEditError] = useState('');
  const [imageList, setImageList] = useState<string[]>([]);
  const [editUploading, setEditUploading] = useState(false);

  useEffect(() => {
    loadData();
    imageService.list().then(setImageList).catch(() => {});
  }, []);

  const loadData = async () => {
    try {
      const [prods, cls] = await Promise.all([productService.getAll(), clientService.getAll()]);
      setProducts(prods);
      setClients(cls);
    } catch (err) {
      console.error('Failed to load data', err);
    } finally {
      setLoading(false);
    }
  };

  // Extract unique values for filter dropdowns
  const categories = useMemo(() => [...new Set(products.map((p) => p.category).filter(Boolean))].sort(), [products]);
  const providers = useMemo(() => [...new Set(products.map((p) => p.provider).filter(Boolean))].sort(), [products]);
  const locations = useMemo(() => [...new Set(products.map((p) => p.location).filter((l): l is string => !!l))].sort(), [products]);

  // Filtered products
  const filtered = useMemo(() => {
    return products.filter((p) => {
      // Text search
      if (search) {
        const q = search.toLowerCase();
        const matchesSearch = p.productName.toLowerCase().includes(q) ||
          p.category.toLowerCase().includes(q) ||
          p.provider.toLowerCase().includes(q) ||
          (p.description || '').toLowerCase().includes(q) ||
          (p.location || '').toLowerCase().includes(q) ||
          String(p.id).includes(q);
        if (!matchesSearch) return false;
      }
      // Category
      if (filterCategory && p.category !== filterCategory) return false;
      // Provider
      if (filterProvider && p.provider !== filterProvider) return false;
      // Location
      if (filterLocation && p.location !== filterLocation) return false;
      // Qty range
      if (filterQtyMin && p.productQuantity < parseInt(filterQtyMin)) return false;
      if (filterQtyMax && p.productQuantity > parseInt(filterQtyMax)) return false;
      // Price range
      if (filterPriceMin && Number(p.price) < parseFloat(filterPriceMin)) return false;
      if (filterPriceMax && Number(p.price) > parseFloat(filterPriceMax)) return false;
      return true;
    });
  }, [products, search, filterCategory, filterProvider, filterLocation, filterQtyMin, filterQtyMax, filterPriceMin, filterPriceMax]);

  const clearFilters = () => {
    setSearch('');
    setFilterCategory('');
    setFilterProvider('');
    setFilterLocation('');
    setFilterQtyMin('');
    setFilterQtyMax('');
    setFilterPriceMin('');
    setFilterPriceMax('');
  };

  const hasActiveFilters = filterCategory || filterProvider || filterLocation || filterQtyMin || filterQtyMax || filterPriceMin || filterPriceMax;

  const handleSell = async (e: FormEvent) => {
    e.preventDefault();
    setSellError('');
    if (!selected) return;
    try {
      if (sellClientId) {
        await saleService.createWithClient(selected.id!, sellClientId, sellQty);
      } else {
        await saleService.create(selected.id!, sellQty);
      }
      setShowSell(false);
      setSellQty(1);
      setSellClientId(0);
      setSelected(null);
      await loadData();
    } catch (err) {
      setSellError(getErrorMessage(err, 'Failed to create sale'));
    }
  };

  const handleEdit = async (e: FormEvent) => {
    e.preventDefault();
    setEditError('');
    if (!editForm) return;
    try {
      await productService.update(editForm.id!, editForm);
      setShowEdit(false);
      setEditForm(null);
      const updated = await productService.getAll();
      setProducts(updated);
      const sel = updated.find((p) => p.id === editForm.id);
      if (sel) setSelected(sel);
    } catch (err) {
      setEditError(getErrorMessage(err, 'Failed to update product'));
    }
  };

  const handleDelete = async () => {
    if (!selected) return;
    if (!window.confirm(`Delete "${selected.productName}"?`)) return;
    try {
      await productService.delete(selected.id!);
      setSelected(null);
      await loadData();
    } catch (err) {
      console.error('Failed to delete', err);
    }
  };

  const getQtyClass = (qty: number) => {
    if (qty < 2) return 'text-danger';
    if (qty < 5) return 'text-warning';
    return '';
  };

  if (loading) return <div className="page-loading">Loading inventory...</div>;

  return (
    <div className="page">
      <div className="inventory-layout">
        {/* Left: Table */}
        <div className="inventory-table-panel">
          <div className="page-title-row">
            <div className="page-title-icon inv">INV</div>
            <div>
              <h1>Inventory</h1>
              <p className="page-subtitle">Click on a product to view details</p>
            </div>
          </div>

          {/* Search + Filter Toggle */}
          <div className="inventory-toolbar">
            <input type="text" className="search-input" placeholder="Search by name, category, ID, provider..." value={search} onChange={(e) => setSearch(e.target.value)} />
            <button className={`btn btn-filter ${showFilters ? 'active' : ''}`} onClick={() => setShowFilters(!showFilters)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
              Filters {hasActiveFilters ? '*' : ''}
            </button>
            {hasActiveFilters && (
              <button className="btn btn-sm btn-secondary" onClick={clearFilters}>Clear All</button>
            )}
          </div>

          {/* Filter Panel */}
          {showFilters && (
            <div className="filter-panel">
              <div className="filter-grid">
                <div className="filter-item">
                  <label>Category</label>
                  <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)}>
                    <option value="">All Categories</option>
                    {categories.map((c) => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
                <div className="filter-item">
                  <label>Provider</label>
                  <select value={filterProvider} onChange={(e) => setFilterProvider(e.target.value)}>
                    <option value="">All Providers</option>
                    {providers.map((p) => <option key={p} value={p}>{p}</option>)}
                  </select>
                </div>
                <div className="filter-item">
                  <label>Location</label>
                  <select value={filterLocation} onChange={(e) => setFilterLocation(e.target.value)}>
                    <option value="">All Locations</option>
                    {locations.map((l) => <option key={l} value={l}>{l}</option>)}
                  </select>
                </div>
                <div className="filter-item">
                  <label>Price Range</label>
                  <div className="filter-range">
                    <input type="number" placeholder="Min" value={filterPriceMin} onChange={(e) => setFilterPriceMin(e.target.value)} />
                    <span>–</span>
                    <input type="number" placeholder="Max" value={filterPriceMax} onChange={(e) => setFilterPriceMax(e.target.value)} />
                  </div>
                </div>
                <div className="filter-item">
                  <label>Quantity Range</label>
                  <div className="filter-range">
                    <input type="number" placeholder="Min" value={filterQtyMin} onChange={(e) => setFilterQtyMin(e.target.value)} />
                    <span>–</span>
                    <input type="number" placeholder="Max" value={filterQtyMax} onChange={(e) => setFilterQtyMax(e.target.value)} />
                  </div>
                </div>
              </div>
              <div className="filter-info">
                Showing {filtered.length} of {products.length} products
              </div>
            </div>
          )}

          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Provider</th>
                  <th>Category</th>
                  <th>Location</th>
                  <th>Price</th>
                  <th>Qty</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((p) => (
                  <tr key={p.id} onClick={() => setSelected(p)} className={selected?.id === p.id ? 'row-selected' : ''}>
                    <td>{p.id}</td>
                    <td>{p.productName}</td>
                    <td>{p.provider}</td>
                    <td><span className="category-badge">{p.category}</span></td>
                    <td>{p.location || '—'}</td>
                    <td>${Number(p.price).toFixed(2)}</td>
                    <td className={getQtyClass(p.productQuantity)}><strong>{p.productQuantity}</strong></td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr><td colSpan={7} className="text-center" style={{ padding: '2rem', color: '#999' }}>No products match your filters</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right: Detail Panel */}
        <div className="inventory-detail-panel">
          <h2>Product Details</h2>
          {selected ? (
            <div className="product-detail">
              <div className="detail-image-container">
                {imageService.toSrc(selected.imagePath) ? (
                  <img key={selected.id} src={imageService.toSrc(selected.imagePath)!} alt={selected.productName} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                ) : (
                  <div className="detail-image-placeholder">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" width="64" height="64"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                  </div>
                )}
              </div>
              <h3>{selected.productName}</h3>
              <p className="detail-meta">Provider: {selected.provider} &middot; Category: {selected.category}</p>
              {selected.location && <p className="detail-meta">Location: {selected.location}</p>}
              {selected.description && <p className="detail-description">Description: {selected.description}</p>}

              <div className="detail-price-row">
                <div className="detail-price-badge">
                  {admin && <>Cost: ${Number(selected.costPrice).toFixed(2)} | </>}
                  Sale: ${Number(selected.price).toFixed(2)}
                  {admin && <> | Profit: ${(Number(selected.price) - Number(selected.costPrice)).toFixed(2)}</>}
                </div>
                <div className={`detail-stock-badge ${selected.productQuantity < 2 ? 'stock-critical' : selected.productQuantity < 5 ? 'stock-low' : ''}`}>
                  Qty: {selected.productQuantity}
                </div>
              </div>

              <div className="detail-actions">
                <button className="btn btn-sell" onClick={() => { setShowSell(true); setSellQty(1); setSellClientId(0); setSellError(''); }}>SELL</button>
                {admin && <button className="btn btn-edit" onClick={() => { setEditForm({ ...selected }); setShowEdit(true); setEditError(''); }}>EDIT</button>}
                {admin && <button className="btn btn-delete" onClick={handleDelete}>DELETE</button>}
              </div>
            </div>
          ) : (
            <div className="detail-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" width="48" height="48"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
              <p>Select a product to view details</p>
            </div>
          )}
        </div>
      </div>

      {/* Sell Modal */}
      {showSell && selected && (
        <div className="modal-overlay" onClick={() => setShowSell(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Sell: {selected.productName}</h2>
            <p className="modal-subtitle">Available stock: {selected.productQuantity}</p>
            <form onSubmit={handleSell}>
              <div className="form-group">
                <label>Quantity</label>
                <input type="number" min="1" max={selected.productQuantity} value={sellQty} onChange={(e) => setSellQty(e.target.value === '' ? 0 : parseInt(e.target.value))} required />
              </div>
              <div className="form-group">
                <label>Client (optional)</label>
                <select value={sellClientId} onChange={(e) => setSellClientId(parseInt(e.target.value))}>
                  <option value={0}>Walk-in / No client</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>{c.firstName} {c.lastName}</option>
                  ))}
                </select>
              </div>
              <div className="sell-total">
                Total: <strong>${(Number(selected.price) * sellQty).toFixed(2)}</strong>
              </div>
              {sellError && <div className="error-message">{sellError}</div>}
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowSell(false)}>Cancel</button>
                <button type="submit" className="btn btn-sell">Confirm Sale</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Modal - includes image and location */}
      {showEdit && editForm && (
        <div className="modal-overlay" onClick={() => setShowEdit(false)}>
          <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
            <h2>Edit Product</h2>
            <form onSubmit={handleEdit}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Product Name</label>
                  <input type="text" value={editForm.productName} onChange={(e) => setEditForm({ ...editForm, productName: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Provider</label>
                  <input type="text" value={editForm.provider} onChange={(e) => setEditForm({ ...editForm, provider: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Category</label>
                  <input type="text" value={editForm.category} onChange={(e) => setEditForm({ ...editForm, category: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Location</label>
                  <input type="text" value={editForm.location || ''} onChange={(e) => setEditForm({ ...editForm, location: e.target.value })} placeholder="e.g. Warehouse A, Shelf 3" />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <input type="text" value={editForm.description || ''} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Cost Price</label>
                  <input type="number" step="0.01" min="0" value={editForm.costPrice} onChange={(e) => setEditForm({ ...editForm, costPrice: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="form-group">
                  <label>Sale Price</label>
                  <input type="number" step="0.01" min="0" value={editForm.price} onChange={(e) => setEditForm({ ...editForm, price: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="form-group">
                  <label>Quantity</label>
                  <input type="number" min="0" value={editForm.productQuantity} onChange={(e) => setEditForm({ ...editForm, productQuantity: parseInt(e.target.value) || 0 })} />
                </div>
                <div className="form-group full-width">
                  <label>Product Image</label>
                  <div className="image-pick-row">
                    <label className={`btn btn-browse ${editUploading ? 'btn-disabled' : ''}`}>
                      {editUploading ? 'Uploading...' : '📁 Browse...'}
                      <input type="file" accept="image/*" style={{ display: 'none' }} disabled={editUploading}
                        onChange={async (e) => {
                          const file = e.target.files?.[0];
                          if (!file) return;
                          setEditUploading(true);
                          try {
                            const filename = await imageService.upload(file);
                            setEditForm({ ...editForm, imagePath: filename });
                            setImageList(prev => prev.includes(filename) ? prev : [...prev, filename].sort());
                          } catch (err: any) {
                            setEditError('Upload failed: ' + (err?.message || 'unknown error'));
                          } finally {
                            setEditUploading(false);
                            e.target.value = '';
                          }
                        }}
                      />
                    </label>
                    <select
                      value={editForm.imagePath}
                      onChange={(e) => setEditForm({ ...editForm, imagePath: e.target.value })}
                      className="image-select"
                    >
                      <option value="">— select existing —</option>
                      {imageList.map(name => (
                        <option key={name} value={name}>{name}</option>
                      ))}
                    </select>
                    <button type="button" className="btn btn-sm" onClick={() => imageService.list().then(setImageList).catch(() => {})} title="Refresh list">↺</button>
                  </div>
                  {editForm.imagePath && imageService.toSrc(editForm.imagePath) && (
                    <div className="edit-image-preview" style={{ marginTop: '0.5rem' }}>
                      <img key={editForm.imagePath} src={imageService.toSrc(editForm.imagePath)!} alt="Preview" style={{ maxHeight: 80, borderRadius: 6 }} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                    </div>
                  )}
                </div>
              </div>
              {editError && <div className="error-message">{editError}</div>}
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowEdit(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
