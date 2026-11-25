import { STATUS_LABEL } from '../utils/expiry.js';

// An item with no expiry date has status "none" for filtering (REQ-026), but shows the Valid
// indicator (REQ-012, Decided Q-006). The status itself always comes from the API (item.status).
const DISPLAY_STATUS = { none: 'valid' };

export default function ExpiryBadge({ status }) {
  const shown = DISPLAY_STATUS[status] ?? status;
  return <span className={`badge badge-${shown}`}>{STATUS_LABEL[shown]}</span>;
}
