import { describe, expect, it } from 'vitest';
import { imageService } from './imageService';

describe('image addresses', () => {
  it('serve a stored file name from the images folder', () => {
    expect(imageService.toSrc('radio.jpg')).toBe('/api/images/serve/radio.jpg');
  });

  it('read the file name out of paths saved by the desktop version', () => {
    expect(imageService.toSrc('product-images/1a34f818.jpg')).toBe('/api/images/serve/1a34f818.jpg');
    expect(imageService.toSrc('/product-images/1a34f818.jpg')).toBe('/api/images/serve/1a34f818.jpg');
  });

  it('keep names with spaces and symbols whole, without renaming them', () => {
    expect(imageService.toSrc('radio #2 (50%).png')).toBe('/api/images/serve/radio%20%232%20(50%25).png');
    expect(imageService.toSrc('cámara.jpg')).toBe('/api/images/serve/c%C3%A1mara.jpg');
  });

  it('leave web addresses as they are', () => {
    expect(imageService.toSrc('https://example.com/a.jpg')).toBe('https://example.com/a.jpg');
  });

  it('return nothing when there is no image', () => {
    expect(imageService.toSrc(undefined)).toBeNull();
    expect(imageService.toSrc('')).toBeNull();
  });
});
