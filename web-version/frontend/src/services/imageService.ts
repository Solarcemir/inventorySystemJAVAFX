import api from './api';

export const imageService = {
  /**
   * Upload a file via the browser file picker → saves to server's images folder.
   * Returns the saved filename (e.g. "samsung-tv.jpg").
   */
  upload: async (file: File): Promise<string> => {
    const form = new FormData();
    form.append('file', file);
    // Use native fetch instead of Axios so the browser sets the correct
    // multipart/form-data boundary automatically (Axios's default
    // Content-Type: application/json header interferes with FormData).
    const res = await fetch('/api/images/upload', { method: 'POST', body: form });
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      throw new Error(body?.message || res.statusText);
    }
    const data = await res.json();
    return data.filename;
  },

  /** Returns all image filenames currently in the server's images folder. */
  list: async (): Promise<string[]> => {
    const res = await api.get<string[]>('/api/images/list');
    return res.data;
  },

  /** Returns the absolute path of the images folder on the server PC. */
  getFolder: async (): Promise<string> => {
    const res = await api.get<string>('/api/images/folder');
    return res.data;
  },

  /**
   * Converts a stored imagePath to a displayable <img src>.
   * - "photo.jpg"           → /api/images/serve/photo.jpg
   * - "/product-images/x"  → /api/images/serve/x  (old upload format)
   * - "https://..."         → used as-is
   */
  toSrc: (imagePath: string | undefined): string | null => {
    if (!imagePath) return null;
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) return imagePath;
    // Extract just the filename from any stored path format
    const filename = imagePath.split('/').pop() || imagePath.split('\\').pop() || '';
    if (!filename) return null;
    // Encoded so names with "#", "?" or "%" still reach the server whole
    return `/api/images/serve/${encodeURIComponent(filename)}`;
  },
};
