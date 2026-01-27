// Utility functions

type StoredPreferences = {
  currency?: string;
  numberFormat?: 'Indian' | 'International';
  compactNumbers?: boolean;
  showEmoji?: boolean;
  dashboard?: {
    showNetWorth?: boolean;
    showRecommendations?: boolean;
    showUpcomingEvents?: boolean;
    showGoalProgress?: boolean;
  };
};

const defaultPreferences: Required<StoredPreferences> = {
  currency: 'INR',
  numberFormat: 'Indian',
  compactNumbers: true,
  showEmoji: true,
  dashboard: {
    showNetWorth: true,
    showRecommendations: true,
    showUpcomingEvents: true,
    showGoalProgress: true,
  },
};

export function getUserPreferences(): Required<StoredPreferences> {
  if (typeof window === 'undefined') {
    return defaultPreferences;
  }
  try {
    const raw = localStorage.getItem('retyrment_preferences');
    if (!raw) return defaultPreferences;
    const parsed = JSON.parse(raw) as StoredPreferences;
    return {
      ...defaultPreferences,
      ...parsed,
      dashboard: {
        ...defaultPreferences.dashboard,
        ...(parsed.dashboard || {}),
      },
    };
  } catch {
    return defaultPreferences;
  }
}

export function getCurrencySymbol(): string {
  const prefs = getUserPreferences();
  const locale = prefs.numberFormat === 'International' ? 'en-US' : 'en-IN';
  const formatted = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: prefs.currency,
    currencyDisplay: 'narrowSymbol',
    maximumFractionDigits: 0,
  }).format(0);
  return formatted.replace(/[0-9.,\s]/g, '') || prefs.currency;
}

type FormatCurrencyOptions = {
  compact?: boolean;
  currency?: string;
  numberFormat?: 'Indian' | 'International';
};

export function formatCurrency(amount: number, compactOrOptions?: boolean | FormatCurrencyOptions): string {
  if (amount === null || amount === undefined || isNaN(amount)) return '₹0';

  const prefs = getUserPreferences();
  const options = typeof compactOrOptions === 'object' ? compactOrOptions : undefined;
  const compactRequested = typeof compactOrOptions === 'boolean'
    ? compactOrOptions
    : options?.compact;
  const compact = compactRequested === undefined
    ? prefs.compactNumbers
    : compactRequested && prefs.compactNumbers;
  const currency = options?.currency ?? prefs.currency;
  const numberFormat = options?.numberFormat ?? prefs.numberFormat;
  const locale = numberFormat === 'International' ? 'en-US' : 'en-IN';

  if (compact) {
    if (currency === 'INR' && numberFormat === 'Indian') {
      if (amount >= 10000000) {
        return `₹${(amount / 10000000).toFixed(2)}Cr`;
      } else if (amount >= 100000) {
        return `₹${(amount / 100000).toFixed(2)}L`;
      } else if (amount >= 1000) {
        return `₹${(amount / 1000).toFixed(1)}K`;
      }
    }
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      notation: 'compact',
      compactDisplay: 'short',
      maximumFractionDigits: 1,
    }).format(amount);
  }

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function numberToWords(value: number): string {
  if (!value || value < 0) return '';
  const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten',
    'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
  const tens = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
  const toWordsBelowThousand = (num: number) => {
    let str = '';
    if (num >= 100) {
      str += `${ones[Math.floor(num / 100)]} Hundred `;
      num %= 100;
    }
    if (num >= 20) {
      str += `${tens[Math.floor(num / 10)]} `;
      num %= 10;
    }
    if (num > 0) {
      str += `${ones[num]} `;
    }
    return str.trim();
  };
  const crore = Math.floor(value / 10000000);
  const lakh = Math.floor((value % 10000000) / 100000);
  const thousand = Math.floor((value % 100000) / 1000);
  const rest = value % 1000;
  const parts = [];
  if (crore) parts.push(`${toWordsBelowThousand(crore)} Crore`);
  if (lakh) parts.push(`${toWordsBelowThousand(lakh)} Lakh`);
  if (thousand) parts.push(`${toWordsBelowThousand(thousand)} Thousand`);
  if (rest) parts.push(toWordsBelowThousand(rest));
  return parts.join(' ').trim();
}

export function amountInWordsHelper(value?: number): string | undefined {
  if (!value || value <= 0) return undefined;
  const words = numberToWords(Math.round(value));
  return words ? `In words: ${words} only` : undefined;
}

export function formatDate(date: string | Date): string {
  if (!date) return '';
  const d = new Date(date);
  if (Number.isNaN(d.getTime())) return '';

  const prefs = getUserPreferences();
  const year = d.getFullYear();
  const month = d.getMonth() + 1;
  const day = d.getDate();

  switch (prefs.dateFormat) {
    case 'MM/DD/YYYY':
      return `${String(month).padStart(2, '0')}/${String(day).padStart(2, '0')}/${year}`;
    case 'YYYY-MM-DD':
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    case 'DD/MM/YYYY':
    default:
      return `${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}/${year}`;
  }
}

export function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`;
}

export function classNames(...classes: (string | boolean | undefined)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function getInitials(name: string): string {
  return name
    .split(' ')
    .map(part => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}
