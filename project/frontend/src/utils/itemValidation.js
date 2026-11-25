// Client-side checks that mirror the backend create rules (ItemValidator). The backend stays authoritative.
import { CATEGORIES } from '../data/categories.js';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function isRealIsoDate(value) {
  if (!ISO_DATE.test(value)) return false;
  const [y, m, d] = value.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  return date.getUTCFullYear() === y && date.getUTCMonth() === m - 1 && date.getUTCDate() === d;
}

// Today in the browser's local calendar, as yyyy-MM-dd. Lexical comparison of these strings is date order.
export function todayIso(now = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return [now.getFullYear(), pad(now.getMonth() + 1), pad(now.getDate())].join('-');
}

// Returns one message per invalid field; an empty object means the form may be sent.
export function validateItemForm(values, today = todayIso()) {
  const errors = {};

  if (!values.name.trim()) {
    errors.name = 'Name is required.';
  }

  const quantity = String(values.quantity).trim();
  if (!quantity) {
    errors.quantity = 'Quantity is required.';
  } else if (!/^\d+$/.test(quantity) || Number(quantity) < 1) {
    errors.quantity = 'Quantity must be a whole number greater than 0.';
  }

  if (!CATEGORIES.includes(values.category)) {
    errors.category = 'Choose a category.';
  }

  if (!values.purchaseDate) {
    errors.purchaseDate = 'Purchase date is required.';
  } else if (!isRealIsoDate(values.purchaseDate)) {
    errors.purchaseDate = 'Enter a valid purchase date.';
  } else if (values.purchaseDate > today) {
    errors.purchaseDate = 'Purchase date must not be later than today.';
  }

  if (values.expiryDate) {
    if (!isRealIsoDate(values.expiryDate)) {
      errors.expiryDate = 'Enter a valid expiry date.';
    } else if (!errors.purchaseDate && values.purchaseDate > values.expiryDate) {
      errors.purchaseDate = 'Purchase date must not be after the expiry date.';
    }
  }

  return errors;
}

// Builds the POST body. The name is sent as typed (the backend stores it untrimmed, A-003).
// expiryDate is omitted when empty.
export function toCreatePayload(values) {
  const payload = {
    name: values.name,
    quantity: Number(values.quantity),
    category: values.category,
    purchaseDate: values.purchaseDate,
  };
  if (values.expiryDate) {
    payload.expiryDate = values.expiryDate;
  }
  return payload;
}
