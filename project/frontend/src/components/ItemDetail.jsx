import ExpiryBadge from './ExpiryBadge.jsx';

// Read-only view of every stored field of one item (REQ-003). Edit and delete stay on the inventory list,
// so this view does not repeat those flows. The item comes from the shared list, so it is never stale.
export default function ItemDetail({ item, onBack }) {
  if (!item) {
    return (
      <section className="page">
        <header className="page-top">
          <div className="page-header">
            <h1>Item details</h1>
          </div>
          <button type="button" className="btn btn-secondary" onClick={onBack}>
            Back to inventory
          </button>
        </header>
        <p className="muted empty">This item is no longer in your pantry.</p>
      </section>
    );
  }

  return (
    <section className="page">
      <header className="page-top">
        <div className="page-header">
          <h1>{item.name}</h1>
          <p className="muted">Item details</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={onBack}>
          Back to inventory
        </button>
      </header>

      <dl className="card detail-list">
        <div className="list-row">
          <dt>Name</dt>
          <dd>{item.name}</dd>
        </div>
        <div className="list-row">
          <dt>Quantity</dt>
          <dd>{item.quantity}</dd>
        </div>
        <div className="list-row">
          <dt>Category</dt>
          <dd>{item.category}</dd>
        </div>
        <div className="list-row">
          <dt>Purchase date</dt>
          <dd>{item.purchaseDate ?? 'Not recorded'}</dd>
        </div>
        <div className="list-row">
          <dt>Expiry date</dt>
          <dd>{item.expiryDate ?? 'No expiry date'}</dd>
        </div>
        <div className="list-row">
          <dt>Status</dt>
          <dd><ExpiryBadge status={item.status ?? 'none'} /></dd>
        </div>
      </dl>
    </section>
  );
}
