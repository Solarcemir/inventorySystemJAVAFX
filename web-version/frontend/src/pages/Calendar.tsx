import { useEffect, useState, useMemo } from 'react';
import { saleService } from '../services/saleService';
import { calendarNoteService, CalendarNote } from '../services/calendarNoteService';
import { isAdmin } from '../services/authService';
import { Sale } from '../types';

type NotesMap = Record<string, CalendarNote[]>; // key: "YYYY-MM-DD"

export default function Calendar() {
  const admin = isAdmin();
  const [sales, setSales] = useState<Sale[]>([]);
  const [notes, setNotes] = useState<NotesMap>({});
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());

  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  const [newNoteText, setNewNoteText] = useState('');

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      const [s, allNotes] = await Promise.all([
        saleService.getAll(),
        calendarNoteService.getAll(),
      ]);
      setSales(s);
      // Group notes by dateKey
      const map: NotesMap = {};
      allNotes.forEach((n) => {
        if (!map[n.dateKey]) map[n.dateKey] = [];
        map[n.dateKey].push(n);
      });
      setNotes(map);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const monthName = currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();

  const getDateKey = (day: number) =>
    `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

  const salesByDay = useMemo(() => {
    const map: Record<number, { count: number; total: number }> = {};
    sales.forEach((s) => {
      const d = new Date(s.saleDate);
      if (d.getFullYear() === year && d.getMonth() === month) {
        const day = d.getDate();
        if (!map[day]) map[day] = { count: 0, total: 0 };
        map[day].count += 1;
        map[day].total += Number(s.totalAmount || 0);
      }
    });
    return map;
  }, [sales, year, month]);

  const monthTotal = Object.values(salesByDay).reduce((sum, d) => sum + d.total, 0);
  const monthSalesCount = Object.values(salesByDay).reduce((sum, d) => sum + d.count, 0);

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));
  const goToday = () => setCurrentDate(new Date());

  const handleAddNote = async () => {
    if (!newNoteText.trim() || selectedDay === null) return;
    const key = getDateKey(selectedDay);
    try {
      const created = await calendarNoteService.create(key, newNoteText.trim());
      setNotes((prev) => ({ ...prev, [key]: [...(prev[key] || []), created] }));
      setNewNoteText('');
    } catch (err) {
      console.error('Failed to save note', err);
    }
  };

  const handleDeleteNote = async (dateKey: string, noteId: number) => {
    try {
      await calendarNoteService.delete(noteId);
      setNotes((prev) => ({
        ...prev,
        [dateKey]: (prev[dateKey] || []).filter((n) => n.id !== noteId),
      }));
    } catch (err) {
      console.error('Failed to delete note', err);
    }
  };

  const cells = [];
  for (let i = 0; i < firstDayOfWeek; i++) cells.push(<div key={`empty-${i}`} className="cal-cell empty" />);
  for (let day = 1; day <= daysInMonth; day++) {
    const info = salesByDay[day];
    const dayNotes = notes[getDateKey(day)] || [];
    const isToday = day === new Date().getDate() && month === new Date().getMonth() && year === new Date().getFullYear();
    cells.push(
      <div
        key={day}
        className={`cal-cell ${isToday ? 'today' : ''} ${info ? 'has-sales' : ''} ${dayNotes.length > 0 ? 'has-events' : ''}`}
        onClick={() => { setSelectedDay(day); setNewNoteText(''); }}
      >
        <span className="cal-day">{day}</span>
        {info && (
          <div className="cal-info">
            <span className="cal-count">{info.count} sale{info.count > 1 ? 's' : ''}</span>
            {admin && <span className="cal-total">${info.total.toFixed(0)}</span>}
          </div>
        )}
        {dayNotes.length > 0 && (
          <div className="cal-events-preview">
            {dayNotes.slice(0, 2).map((n) => (
              <span key={n.id} className="cal-event-dot" title={n.text}>
                {n.text.length > 12 ? n.text.slice(0, 12) + '…' : n.text}
              </span>
            ))}
            {dayNotes.length > 2 && <span className="cal-event-more">+{dayNotes.length - 2}</span>}
          </div>
        )}
      </div>
    );
  }

  if (loading) return <div className="page-loading">Loading calendar...</div>;

  return (
    <div className="page">
      <div className="page-title-row">
        <div className="page-title-icon" style={{ background: '#e67e22' }}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <div>
          <h1>Sales Calendar</h1>
          <p className="page-subtitle">View sales activity and events by day</p>
        </div>
      </div>

      <div className="cal-header">
        <button className="btn btn-secondary" onClick={prevMonth}>&larr;</button>
        <h2>{monthName}</h2>
        <button className="btn btn-secondary" onClick={goToday}>Today</button>
        <button className="btn btn-secondary" onClick={nextMonth}>&rarr;</button>
      </div>

      {admin && (
        <div className="cal-summary">
          <span><strong>{monthSalesCount}</strong> sales this month</span>
          <span>Total Revenue: <strong className="text-accent">${monthTotal.toFixed(2)}</strong></span>
        </div>
      )}

      <div className="cal-grid">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
          <div key={d} className="cal-weekday">{d}</div>
        ))}
        {cells}
      </div>

      {selectedDay !== null && (
        <div className="modal-overlay" onClick={() => setSelectedDay(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{monthName.split(' ')[0]} {selectedDay}, {year}</h2>

            {/* Revenue — admin only */}
            {admin && salesByDay[selectedDay] && (
              <div className="cal-day-sales-info">
                <strong>{salesByDay[selectedDay].count}</strong> sale{salesByDay[selectedDay].count > 1 ? 's' : ''} — Revenue: <strong className="text-accent">${salesByDay[selectedDay].total.toFixed(2)}</strong>
              </div>
            )}
            {/* Seller: show sale count only, no revenue */}
            {!admin && salesByDay[selectedDay] && (
              <div className="cal-day-sales-info">
                <strong>{salesByDay[selectedDay].count}</strong> sale{salesByDay[selectedDay].count > 1 ? 's' : ''} this day
              </div>
            )}

            <h3 style={{ fontSize: '0.95rem', margin: '1rem 0 0.5rem' }}>Events & Notes</h3>
            {(notes[getDateKey(selectedDay)] || []).length === 0 ? (
              <p style={{ color: '#999', fontSize: '0.85rem', marginBottom: '0.75rem' }}>No notes for this day.</p>
            ) : (
              <div className="cal-day-events">
                {(notes[getDateKey(selectedDay)] || []).map((n) => (
                  <div key={n.id} className="cal-day-event">
                    <span>{n.text}</span>
                    <button className="cal-event-delete" onClick={() => handleDeleteNote(getDateKey(selectedDay), n.id)}>&times;</button>
                  </div>
                ))}
              </div>
            )}

            <div className="cal-add-event">
              <input
                type="text"
                placeholder="Add a note..."
                value={newNoteText}
                onChange={(e) => setNewNoteText(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleAddNote(); }}
                autoFocus
              />
              <button className="btn btn-primary" onClick={handleAddNote} disabled={!newNoteText.trim()}>Add</button>
            </div>

            <div className="form-actions">
              <button className="btn btn-secondary" onClick={() => setSelectedDay(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
