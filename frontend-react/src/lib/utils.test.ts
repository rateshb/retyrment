import { describe, it, expect } from 'vitest';
import {
  amountInWordsHelper,
  classNames,
  formatCurrency,
  formatDate,
  formatPercentage,
  getInitials,
  numberToWords,
} from './utils';

describe('utils', () => {
  it('formats currency in compact notation', () => {
    expect(formatCurrency(1_000, true)).toBe('₹1.0K');
    expect(formatCurrency(1_00_000, true)).toBe('₹1.00L');
    expect(formatCurrency(1_00_00_000, true)).toBe('₹1.00Cr');
  });

  it('formats currency in full notation', () => {
    expect(formatCurrency(1_000)).toBe('₹1,000');
    expect(formatCurrency(1_00_000)).toBe('₹1,00,000');
  });

  it('converts numbers to words for Indian format', () => {
    expect(numberToWords(100000)).toBe('One Lakh');
    expect(numberToWords(150000)).toBe('One Lakh Fifty Thousand');
  });

  it('formats amount in words helper', () => {
    expect(amountInWordsHelper(100000)).toBe('In words: One Lakh only');
    expect(amountInWordsHelper(0)).toBeUndefined();
  });

  it('formats dates and percentages', () => {
    expect(formatPercentage(7.15)).toBe('7.2%');
    expect(formatDate('2025-01-01')).toContain('2025');
  });

  it('builds class names', () => {
    expect(classNames('a', false, 'b', undefined, 'c')).toBe('a b c');
  });

  it('gets initials', () => {
    expect(getInitials('John Doe')).toBe('JD');
    expect(getInitials('Retyrment App')).toBe('RA');
  });
});
