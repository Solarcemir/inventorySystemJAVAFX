import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Sessions end when the server restarts or after 12 hours: back to the login screen
api.interceptors.response.use(undefined, (error) => {
  if (error.response?.status === 401) {
    sessionStorage.clear();
    window.location.assign('/');
  }
  return Promise.reject(error);
});

/**
 * Identifies one action, so the server can tell a retry from a new request.
 * Not crypto.randomUUID: it only exists on HTTPS or localhost, and other PCs in the shop use plain HTTP.
 */
export function newIdempotencyKey(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

/** The server's explanation of a failed request, or the fallback when it gave none. */
export function getErrorMessage(err: any, fallback: string): string {
  return err?.response?.data?.message || fallback;
}

export default api;
