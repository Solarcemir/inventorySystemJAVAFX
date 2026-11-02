import { afterEach, describe, expect, it, vi } from 'vitest';
import { getErrorMessage, newIdempotencyKey } from './api';

describe('idempotency keys', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('are 32 hexadecimal characters', () => {
    expect(newIdempotencyKey()).toMatch(/^[0-9a-f]{32}$/);
  });

  it('are different every time', () => {
    const keys = new Set(Array.from({ length: 1000 }, () => newIdempotencyKey()));
    expect(keys.size).toBe(1000);
  });

  // Other PCs open the app over plain HTTP, where browsers leave out crypto.randomUUID
  it('work where crypto.randomUUID does not exist', () => {
    const insecureCrypto = { getRandomValues: crypto.getRandomValues.bind(crypto) };
    vi.stubGlobal('crypto', insecureCrypto);

    expect(newIdempotencyKey()).toMatch(/^[0-9a-f]{32}$/);
  });
});

describe('error messages', () => {
  it('use the message sent by the server', () => {
    const error = { response: { data: { message: 'Stock insuficiente. Disponible: 2' } } };
    expect(getErrorMessage(error, 'Failed')).toBe('Stock insuficiente. Disponible: 2');
  });

  it('fall back when the server sent none', () => {
    expect(getErrorMessage({ response: { data: '' } }, 'Failed')).toBe('Failed');
    expect(getErrorMessage(new Error('Network Error'), 'Failed')).toBe('Failed');
    expect(getErrorMessage(undefined, 'Failed')).toBe('Failed');
  });
});
