// Utility functions

export function formatCurrency(amount: number, compact = false): string {
  if (amount === null || amount === undefined || isNaN(amount)) return '₹0';
  
  if (compact) {
    if (amount >= 10000000) {
      return `₹${(amount / 10000000).toFixed(2)}Cr`;
    } else if (amount >= 100000) {
      return `₹${(amount / 100000).toFixed(2)}L`;
    } else if (amount >= 1000) {
      return `₹${(amount / 1000).toFixed(1)}K`;
    }
  }
  
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
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
  return d.toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
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
