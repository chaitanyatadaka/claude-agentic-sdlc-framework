import { CATEGORIES } from '../data/categories.js';
import { daysUntil } from '../utils/expiry.js';
import ExpiryBadge from '../components/ExpiryBadge.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';

function StatCard({ label, value, hint, tone }) {
  return (
    <div className={`stat-card stat-${tone}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-hint">{hint}</div>
    </div>
  );
}

export default function Dashboard({ items, status, error, correlationId, onRetry }) {
  // Expiry status comes from the API (item.status); the browser does not classify dates here.
  const totalQuantity = items.reduce((sum, item) => sum + item.quantity, 0);
  const expiring = items.filter((i) => i.status === 'expiring');
  const expired = items.filter((i) => i.status === 'expired');

  const byCategory = CATEGORIES.map((category) => {
    const inCategory = items.filter((i) => i.category === category);
    return {
      category,
      count: inCategory.length,
      quantity: inCategory.reduce((sum, i) => sum + i.quantity, 0),
    };
  });
  const maxCount = Math.max(1, ...byCategory.map((c) => c.count));

  return (
    <section className="page">
      <header className="page-header">
        <h1>Dashboard</h1>
        <p className="muted">A quick view of what is in your pantry and what needs using up.</p>
      </header>

      {status === 'loading' && <p className="muted" role="status">Loading your pantry…</p>}
      {status === 'error' && (
        <ErrorMessage message={error} correlationId={correlationId} onRetry={onRetry} />
      )}

      {status === 'ready' && (
        <>
          <div className="stat-grid">
            <StatCard label="Grocery items" value={items.length} hint="Active items" tone="sage" />
            <StatCard label="Total quantity" value={totalQuantity} hint="Across all items" tone="sage" />
            <StatCard label="Expiring soon" value={expiring.length} hint="Within 3 days" tone="amber" />
            <StatCard label="Expired" value={expired.length} hint="Past expiry date" tone="red" />
          </div>

          <div className="two-column">
            <div className="card">
              <h2>Use soon</h2>
              {expiring.length + expired.length === 0 ? (
                <p className="muted">Nothing is expiring soon.</p>
              ) : (
                <ul className="list">
                  {[...expired, ...expiring].map((item) => (
                    <li key={item.id} className="list-row">
                      <span>
                        <strong>{item.name}</strong>
                        <span className="muted"> · {item.quantity} × {item.category}</span>
                      </span>
                      <span className="row-end">
                        <ExpiryBadge status={item.status} />
                        <span className="muted small">
                          {daysUntil(item.expiryDate) < 0 ? `${Math.abs(daysUntil(item.expiryDate))}d ago` : `in ${daysUntil(item.expiryDate)}d`}
                        </span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div className="card">
              <h2>By category</h2>
              <ul className="bars">
                {byCategory.map((c) => (
                  <li key={c.category} className="bar-row">
                    <span className="bar-label">{c.category}</span>
                    <span className="bar-track">
                      <span className="bar-fill" style={{ width: `${(c.count / maxCount) * 100}%` }} />
                    </span>
                    <span className="bar-count">{c.count} · {c.quantity} qty</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </>
      )}
    </section>
  );
}
