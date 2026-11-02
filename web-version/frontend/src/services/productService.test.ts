import { describe, expect, it } from 'vitest';
import { productService } from './productService';
import { Product } from '../types';

const product = (overrides: Partial<Product> = {}): Product => ({
  id: 42,
  productName: 'Pionner bocina x32D',
  provider: 'Pionner',
  category: 'Audio',
  price: 10,
  costPrice: 5,
  productQuantity: 3,
  description: 'Modelo X99Z',
  location: 'Estante 3',
  ...overrides,
});

const matches = (query: string, overrides?: Partial<Product>) =>
  productService.matchesSearch(product(overrides), query);

describe('product search', () => {
  it('finds a code anywhere in the name, in any case', () => {
    expect(matches('X32D')).toBe(true);
    expect(matches('x32d')).toBe(true);
    expect(matches('bocina')).toBe(true);
  });

  it('ignores spaces around the search', () => {
    expect(matches('  x32d  ')).toBe(true);
  });

  it('accepts the words in any order', () => {
    expect(matches('x32d pionner')).toBe(true);
    expect(matches('audio x32d')).toBe(true);
  });

  it('needs every word to appear', () => {
    expect(matches('x32d sony')).toBe(false);
  });

  it('ignores separators and accents', () => {
    expect(matches('kdx35', { productName: 'Radio JVC KD-X35' })).toBe(true);
    expect(matches('kd x35', { productName: 'Radio JVC KD-X35' })).toBe(true);
    expect(matches('camara', { productName: 'Cámara reversa' })).toBe(true);
    expect(matches('CÁMARA', { productName: 'camara reversa' })).toBe(true);
  });

  it('looks in the description, provider, location and ID too', () => {
    expect(matches('x99z')).toBe(true);
    expect(matches('pionner')).toBe(true);
    expect(matches('estante 3')).toBe(true);
    expect(matches('42')).toBe(true);
  });

  it('survives products with missing fields', () => {
    const incomplete = product({ category: null as unknown as string, provider: undefined as unknown as string, description: undefined });
    expect(productService.matchesSearch(incomplete, 'x32d')).toBe(true);
    expect(productService.matchesSearch(incomplete, 'audio')).toBe(false);
  });

  it('shows everything for an empty or symbols-only search', () => {
    expect(matches('')).toBe(true);
    expect(matches('   ')).toBe(true);
    expect(matches('-')).toBe(true);
  });

  it('does not find what is not there', () => {
    expect(matches('zzz')).toBe(false);
  });
});
