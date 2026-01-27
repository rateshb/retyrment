import { describe, it, expect, beforeEach } from 'vitest';
import {
  amountInWordsHelper,
  classNames,
  formatCurrency,
  formatDate,
  formatPercentage,
  getCurrencySymbol,
  getInitials,
  getUserPreferences,
  numberToWords,
} from './utils';

describe('utils', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('formats currency in compact notation', () => {
    expect(formatCurrency(1_000, true)).toBe('₹1.0K');
    expect(formatCurrency(1_00_000, true)).toBe('₹1.00L');
    expect(formatCurrency(1_00_00_000, true)).toBe('₹1.00Cr');
  });

  it('formats currency in full notation', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({ compactNumbers: false }));
    expect(formatCurrency(1_000)).toBe('₹1,000');
    expect(formatCurrency(1_00_000)).toBe('₹1,00,000');
  });

  it('uses user preferences for currency and number format', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({
      currency: 'USD',
      numberFormat: 'International',
      compactNumbers: false,
    }));

    expect(formatCurrency(1000)).toBe('$1,000');
  });

  it('respects explicit currency options over preferences', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({ currency: 'USD' }));
    expect(formatCurrency(1000, { currency: 'INR', compact: false })).toBe('₹1,000');
  });

  it('formats dates based on dateFormat preference', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({ dateFormat: 'YYYY-MM-DD' }));
    expect(formatDate('2025-01-09')).toBe('2025-01-09');

    localStorage.setItem('retyrment_preferences', JSON.stringify({ dateFormat: 'MM/DD/YYYY' }));
    expect(formatDate('2025-01-09')).toBe('01/09/2025');

    localStorage.setItem('retyrment_preferences', JSON.stringify({ dateFormat: 'DD/MM/YYYY' }));
    expect(formatDate('2025-01-09')).toBe('09/01/2025');
  });

  it('returns empty string for invalid dates', () => {
    expect(formatDate('invalid-date')).toBe('');
  });

  it('returns currency symbol from preferences', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({ currency: 'USD', numberFormat: 'International' }));
    expect(getCurrencySymbol()).toBe('$');
  });

  it('merges preferences with defaults', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({ currency: 'EUR' }));
    const prefs = getUserPreferences();
    expect(prefs.currency).toBe('EUR');
    expect(prefs.numberFormat).toBe('Indian');
    expect(prefs.dashboard.showNetWorth).toBe(true);
  });

  it('converts numbers to words for Indian format', () => {
    expect(numberToWords(100000)).toBe('One Lakh');
    expect(numberToWords(150000)).toBe('One Lakh Fifty Thousand');
    expect(numberToWords(0)).toBe('');
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
    expect(getInitials('Solo')).toBe('S');
  });
});
