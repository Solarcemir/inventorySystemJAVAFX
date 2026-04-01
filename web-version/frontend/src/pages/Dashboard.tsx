import { useEffect, useState, useRef } from 'react';
import { productService } from '../services/productService';
import { clientService } from '../services/clientService';
import { saleService } from '../services/saleService';

interface Note {
  id: string;
  text: string;
}

const NOTES_KEY = 'inventory_quick_notes';

function loadNotes(): Note[] {
  try {
    const raw = localStorage.getItem(NOTES_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveNotes(notes: Note[]) {
  localStorage.setItem(NOTES_KEY, JSON.stringify(notes));
}

export default function Dashboard() {
  const [productCount, setProductCount] = useState(0);
  const [clientCount, setClientCount] = useState(0);
  const [salesCount, setSalesCount] = useState(0);
  const [notes, setNotes] = useState<Note[]>(loadNotes);
  const [loading, setLoading] = useState(true);
  const noteRefs = useRef<Map<string, HTMLTextAreaElement>>(new Map());

  useEffect(() => {
    loadCounts();
  }, []);

  const loadCounts = async () => {
    try {
      const [p, c, s] = await Promise.all([
        productService.getCount(),
        clientService.getCount(),
        saleService.getTotalCount(),
      ]);
      setProductCount(p);
      setClientCount(c);
      setSalesCount(s);
    } catch (err) {
      console.error('Failed to load counts', err);
    } finally {
      setLoading(false);
    }
  };

  const addNote = () => {
    const newNote: Note = { id: Date.now().toString(), text: '' };
    const updated = [...notes, newNote];
    setNotes(updated);
    saveNotes(updated);
    setTimeout(() => {
      const el = noteRefs.current.get(newNote.id);
      if (el) el.focus();
    }, 50);
  };

  const updateNote = (id: string, text: string) => {
    const updated = notes.map((n) => (n.id === id ? { ...n, text } : n));
    setNotes(updated);
    saveNotes(updated);
  };

  const deleteNote = (id: string) => {
    const updated = notes.filter((n) => n.id !== id);
    setNotes(updated);
    saveNotes(updated);
  };

  return (
    <div className="page">
      <div className="kpi-cards">
        <div className="kpi-card kpi-blue">
          <div className="kpi-number">{loading ? '...' : productCount}</div>
          <div className="kpi-label">Product Count</div>
          <div className="kpi-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
          </div>
        </div>
        <div className="kpi-card kpi-yellow">
          <div className="kpi-number">{loading ? '...' : clientCount}</div>
          <div className="kpi-label">Clients Count</div>
          <div className="kpi-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
          </div>
        </div>
        <div className="kpi-card kpi-pink">
          <div className="kpi-number">{loading ? '...' : salesCount}</div>
          <div className="kpi-label">Sales Count</div>
          <div className="kpi-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          </div>
        </div>
      </div>

      <div className="notes-section">
        <div className="notes-header">
          <h2>Quick Notes</h2>
          <button className="btn btn-accent" onClick={addNote}>+ New Note</button>
        </div>
        <div className="notes-grid">
          {notes.map((note) => (
            <div key={note.id} className="note-card">
              <button className="note-delete" onClick={() => deleteNote(note.id)} title="Delete note">&times;</button>
              <textarea
                ref={(el) => { if (el) noteRefs.current.set(note.id, el); }}
                value={note.text}
                onChange={(e) => updateNote(note.id, e.target.value)}
                placeholder="Write a note..."
              />
            </div>
          ))}
          {notes.length === 0 && (
            <p className="empty-notes">No notes yet. Click "+ New Note" to get started.</p>
          )}
        </div>
      </div>
    </div>
  );
}
