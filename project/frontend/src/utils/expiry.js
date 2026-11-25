// Expiry classification (REQ-010, REQ-011, Q-003 decided: expiring today is "Expiring soon").
export const EXPIRING_SOON_DAYS = 3;

const DAY_MS = 24 * 60 * 60 * 1000;

export function daysUntil(expiryIso, today = new Date()) {
  if (!expiryIso) return null;
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const [y, m, d] = expiryIso.split('-').map(Number);
  const expiry = new Date(y, m - 1, d);
  return Math.round((expiry - start) / DAY_MS);
}

export function expiryStatus(expiryIso, today = new Date()) {
  const days = daysUntil(expiryIso, today);
  if (days === null) return 'none';
  if (days < 0) return 'expired';
  if (days <= EXPIRING_SOON_DAYS) return 'expiring';
  return 'valid';
}

export const STATUS_LABEL = {
  expired: 'Expired',
  expiring: 'Expiring soon',
  valid: 'Valid',
  none: 'No expiry',
};
