import { STATUS_LABEL } from '../utils/expiry.js';

export default function ExpiryBadge({ status }) {
  return <span className={`badge badge-${status}`}>{STATUS_LABEL[status]}</span>;
}
