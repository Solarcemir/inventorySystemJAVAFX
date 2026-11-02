import api from './api';

export interface CalendarNote {
  id: number;
  dateKey: string;
  text: string;
}

export const calendarNoteService = {
  getAll: async (): Promise<CalendarNote[]> => {
    const res = await api.get<CalendarNote[]>('/api/calendar-notes');
    return res.data;
  },
  create: async (dateKey: string, text: string): Promise<CalendarNote> => {
    const res = await api.post<CalendarNote>('/api/calendar-notes', { dateKey, text });
    return res.data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/calendar-notes/${id}`);
  },
};
