import { useState, FormEvent } from 'react';
import { productService } from '../services/productService';
import { logService } from '../services/logService';
import { Product } from '../types';

const emptyProduct: Product = {
  productName: '',
  provider: '',
  productQuantity: 1,
  category: '',
  price: 0,
  costPrice: 0,
  imagePath: '',
  description: '',
  location: '',
};

export default function AddProduct() {
  const [form, setForm] = useState<Product>(emptyProduct);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const profit = form.price - form.costPrice;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.productName.trim()) { setError('Product name is required'); return; }
    if (form.price < 0) { setError('Sale price cannot be negative'); return; }
    if (form.productQuantity < 1) { setError('Quantity must be at least 1'); return; }

    setSubmitting(true);
    try {
      await productService.create(form);
      await logService.log('ADD_PRODUCT', `Added product "${form.productName}" (Qty: ${form.productQuantity}, Price: $${form.price.toFixed(2)})`);
      setSuccess(true);
      setForm(emptyProduct);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create product');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page">
      <div className="add-product-layout">
        <div className="add-product-form-panel">
          <div className="page-title-row">
            <div className="page-title-icon">+</div>
            <div>
              <h1>Add Product</h1>
              <p className="page-subtitle">Fill in the product details below</p>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Product Name</label>
              <input type="text" value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} placeholder="Enter product name..." required />
            </div>
            <div className="form-group">
              <label>Vendor / Provider</label>
              <input type="text" value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })} placeholder="Enter vendor name..." />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Category</label>
                <input type="text" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} placeholder="Category" />
              </div>
              <div className="form-group">
                <label>Description</label>
                <input type="text" value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Description" />
              </div>
            </div>
            <div className="form-group">
              <label>Location</label>
              <input type="text" value={form.location || ''} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="e.g. Warehouse A, Shelf 3" />
            </div>

            <div className="price-row">
              <div className="price-box cost">
                <span className="price-label">COST PRICE</span>
                <input type="number" step="0.01" min="0" value={form.costPrice} onChange={(e) => setForm({ ...form, costPrice: parseFloat(e.target.value) || 0 })} />
              </div>
              <div className="price-arrow">→</div>
              <div className="price-box sale">
                <span className="price-label">SALE PRICE</span>
                <input type="number" step="0.01" min="0" value={form.price} onChange={(e) => setForm({ ...form, price: parseFloat(e.target.value) || 0 })} />
              </div>
            </div>

            <div className="quantity-row">
              <label>Quantity:</label>
              <input type="number" min="0" max="10000" value={form.productQuantity} onChange={(e) => setForm({ ...form, productQuantity: parseInt(e.target.value) || 0 })} className="quantity-input" />
            </div>

            <div className="form-group">
              <label>Image Path</label>
              <div className="image-input-row">
                <input type="text" value={form.imagePath || ''} onChange={(e) => setForm({ ...form, imagePath: e.target.value })} placeholder="Select product image..." readOnly />
                <label className="btn btn-browse">
                  Browse
                  <input type="file" accept="image/*" style={{ display: 'none' }} onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) setForm({ ...form, imagePath: `product-images/${file.name}` });
                  }} />
                </label>
              </div>
            </div>

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">Product added successfully!</div>}

            <button type="submit" className="btn btn-primary btn-full btn-lg" disabled={submitting}>
              {submitting ? 'Adding...' : 'ADD PRODUCT'}
            </button>
          </form>
        </div>

        <div className="add-product-preview">
          <h2>Live Preview</h2>
          <div className="preview-card">
            <div className="preview-image">
              {form.imagePath ? (
                <img src={form.imagePath} alt="Preview" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
              ) : (
                <div className="preview-placeholder">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                </div>
              )}
            </div>
            <div className="preview-details">
              <h3>{form.productName || 'Product Name'}</h3>
              <p className="preview-meta">{form.provider || 'Vendor'} &middot; {form.category || 'Category'}{form.location ? ` &middot; ${form.location}` : ''}</p>
              <p className="preview-desc">{form.description || 'Description will appear here...'}</p>
              <div className="preview-prices">
                <div className="preview-price cost">
                  <span>COST</span>
                  <strong>${form.costPrice.toFixed(2)}</strong>
                </div>
                <div className="preview-price sale">
                  <span>SALE</span>
                  <strong>${form.price.toFixed(2)}</strong>
                </div>
                <div className="preview-price profit">
                  <span>PROFIT</span>
                  <strong>${profit.toFixed(2)}</strong>
                </div>
              </div>
              <div className="preview-stock">
                {form.productQuantity} units
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
