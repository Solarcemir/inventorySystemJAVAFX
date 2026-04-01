import { useEffect, useState, useMemo } from 'react';
import { saleService } from '../services/saleService';
import { isAdmin } from '../services/authService';
import { Sale } from '../types';

interface CalendarEvent {
  id: string;
  text: string;
}

type EventsMap = Record<string, CalendarEvent[]>; // key: "YYYY-MM-DD"

const EVENTS_KEY = 'cft-calendar-events';

function loadEvents(): EventsMap {
  try {
    return JSON.parse(localStorage.getItem(EVENTS_KEY) || '{}');
  } catch {
    return {};
  }
}

function saveEvents(events: EventsMap) {
  localStorage.setItem(EVENTS_KEY, JSON.stringify(events));
}

export default function Calendar() {
  const admin = isAdmin();
  const [sales, setSales] = useState<Sale[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [events, setEvents] = useState<EventsMap>(loadEvents);

  // Event modal state
  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  const [newEventText, setNewEventText] = useState('');

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try { setSales(await saleService.getAll()); } catch (err) { console.error(err); } finally { setLoading(false); }
  };

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const monthName = currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();

  const getDateKey = (day: number) =>
    `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

  // Group sales by day
  const salesByDay = useMemo(() => {
    const map: Record<number, { count: number; total: number }> = {};
    sales.forEach((s) => {
      const d = new Date(s.saleDate);
      if (d.getFullYear() === year && d.getMonth() === month) {
        const day = d.getDate();
        if (!map[day]) map[day] = { count: 0, total: 0 };
        map[day].count += 1;
        map[day].total += Number(s.totalAmount);
      }
    });
    return map;
  }, [sales, year, month]);

  const monthTotal = Object.values(salesByDay).reduce((sum, d) => sum + d.total, 0);
  const monthSalesCount = Object.values(salesByDay).reduce((sum, d) => sum + d.count, 0);

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));
  const goToday = () => setCurrentDate(new Date());

  const handleAddEvent = () => {
    if (!newEventText.trim() || selectedDay === null) return;
    const key = getDateKey(selectedDay);
    const updated = { ...events };
    if (!updated[key]) updated[key] = [];
    updated[key].push({ id: Date.now().toString(), text: newEventText.trim() });
    setEvents(updated);
    saveEvents(updated);
    setNewEventText('');
  };

  const handleDeleteEvent = (day: number, eventId: string) => {
    const key = getDateKey(day);
    const updated = { ...events };
    updated[key] = (updated[key] || []).filter((e) => e.id !== eventId);
    if (updated[key].length === 0) delete updated[key];
    setEvents(updated);
    saveEvents(updated);
  };

  const cells = [];
  for (let i = 0; i < firstDayOfWeek; i++) cells.push(<div key={`empty-${i}`} className="cal-cell empty" />);
  for (let day = 1; day <= daysInMonth; day++) {
    const info = salesByDay[day];
    const dayEvents = events[getDateKey(day)] || [];
    const isToday = day === new Date().getDate() && month === new Date().getMonth() && year === new Date().getFullYear();
    cells.push(
      <div
        key={day}
        className={`cal-cell ${isToday ? 'today' : ''} ${info ? 'has-sales' : ''} ${dayEvents.length > 0 ? 'has-events' : ''}`}
        onClick={() => { setSelectedDay(day); setNewEventText(''); }}
      >
        <span className="cal-day">{day}</span>
        {info && (
          <div className="cal-info">
            <span className="cal-count">{info.count} sale{info.count > 1 ? 's' : ''}</span>
            <span className="cal-total">${info.total.toFixed(0)}</span>
          </div>
        )}
        {dayEvents.length > 0 && (
          <div className="cal-events-preview">
            {dayEvents.slice(0, 2).map((ev) => (
              <span key={ev.id} className="cal-event-dot" title={ev.text}>
                {ev.text.length > 12 ? ev.text.slice(0, 12) + '…' : ev.text}
              </span>
            ))}
            {dayEvents.length > 2 && <span className="cal-event-more">+{dayEvents.length - 2}</span>}
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

      {/* Day Detail Modal */}
      {selectedDay !== null && (
        <div className="modal-overlay" onClick={() => setSelectedDay(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{monthName.split(' ')[0]} {selectedDay}, {year}</h2>

            {/* Sales info for this day */}
            {salesByDay[selectedDay] && (
              <div className="cal-day-sales-info">
                <strong>{salesByDay[selectedDay].count}</strong> sale{salesByDay[selectedDay].count > 1 ? 's' : ''} — Revenue: <strong className="text-accent">${salesByDay[selectedDay].total.toFixed(2)}</strong>
              </div>
            )}

            {/* Events */}
            <h3 style={{ fontSize: '0.95rem', margin: '1rem 0 0.5rem' }}>Events & Notes</h3>
            {(events[getDateKey(selectedDay)] || []).length === 0 ? (
              <p style={{ color: '#999', fontSize: '0.85rem', marginBottom: '0.75rem' }}>No events for this day.</p>
            ) : (
              <div className="cal-day-events">
                {(events[getDateKey(selectedDay)] || []).map((ev) => (
                  <div key={ev.id} className="cal-day-event">
                    <span>{ev.text}</span>
                    <button className="cal-event-delete" onClick={() => handleDeleteEvent(selectedDay, ev.id)}>&times;</button>
                  </div>
                ))}
              </div>
            )}

            {/* Add event form */}
            <div className="cal-add-event">
              <input
                type="text"
                placeholder="Add an event or note..."
                value={newEventText}
                onChange={(e) => setNewEventText(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleAddEvent(); }}
                autoFocus
              />
              <button className="btn btn-primary" onClick={handleAddEvent} disabled={!newEventText.trim()}>Add</button>
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
