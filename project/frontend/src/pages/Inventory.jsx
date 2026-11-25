import { useMemo, useState } from 'react';
import { CATEGORIES } from '../data/categories.js';
import ExpiryBadge from '../components/ExpiryBadge.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import AddItemDialog from '../components/AddItemDialog.jsx';
import DeleteItemDialog from '../components/DeleteItemDialog.jsx';
import ItemDetail from '../components/ItemDetail.jsx';

const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'expiring', label: 'Expiring soon' },
  { id: 'expired', label: 'Expired' },
  { id: 'valid', label: 'Valid' },
];

export default function Inventory({
  items,
  status,
  error,
  correlationId,
  onRetry,
  onItemSaved,
  onItemUpdated,
  onItemDeleted,
}) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState(null);
  const [itemToDelete, setItemToDelete] = useState(null);
  // Id of the item open in the detail view (REQ-003), or null for the list.
  const [detailId, setDetailId] = useState(null);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return items
      .filter((item) => (q ? item.name.toLowerCase().includes(q) : true))
      .filter((item) => (category ? item.category === category : true))
      .filter((item) => (statusFilter === 'all' ? true : item.status === statusFilter));
  }, [items, query, category, statusFilter]);

  if (detailId !== null) {
    // Looked up in the shared list each render, so the detail follows any refresh.
    const detailItem = items.find((item) => item.id === detailId) ?? null;
    return <ItemDetail item={detailItem} onBack={() => setDetailId(null)} />;
  }

  function renderBody() {
    if (status === 'error') {
      return <ErrorMessage message={error} correlationId={correlationId} onRetry={onRetry} />;
    }
    if (status === 'loading' && items.length === 0) {
      return <p className="muted empty" role="status">Loading items…</p>;
    }
    if (items.length === 0) {
      return <p className="muted empty">Your pantry is empty. Use Add item to save your first grocery item.</p>;
    }
    if (rows.length === 0) {
      return <p className="muted empty">No items match these filters.</p>;
    }
    return (
      <div className="table-scroll">
        <table className="table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Category</th>
              <th className="num">Quantity</th>
              <th>Expiry date</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.id} className={`row-${item.status}`}>
                <td data-label="Item">
                  <button
                    type="button"
                    className="link-button"
                    onClick={() => setDetailId(item.id)}
                  >
                    <strong>{item.name}</strong>
                  </button>
                </td>
                <td data-label="Category">{item.category}</td>
                <td data-label="Quantity" className="num">{item.quantity}</td>
                <td data-label="Expiry date">{item.expiryDate ?? 'No expiry date'}</td>
                <td data-label="Status"><ExpiryBadge status={item.status ?? 'none'} /></td>
                <td data-label="Actions">
                  <div className="row-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      aria-label={`View ${item.name}`}
                      onClick={() => setDetailId(item.id)}
                    >
                      View
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      aria-label={`Edit ${item.name}`}
                      onClick={() => setItemToEdit(item)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger-subtle btn-sm"
                      aria-label={`Delete ${item.name}`}
                      onClick={() => setItemToDelete(item)}
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <section className="page">
      <header className="page-top">
        <div className="page-header">
          <h1>Pantry inventory</h1>
          <p className="muted">Items saved in your pantry.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setDialogOpen(true)}>
          Add item
        </button>
      </header>

      <div className="toolbar">
        <input
          className="input search"
          type="search"
          placeholder="Search items by name"
          aria-label="Search items by name"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select className="input" aria-label="Filter by category" value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <div className="segmented" role="group" aria-label="Filter by expiry status">
          {FILTERS.map((f) => (
            <button
              key={f.id}
              type="button"
              className={`seg ${statusFilter === f.id ? 'is-active' : ''}`}
              onClick={() => setStatusFilter(f.id)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="card table-card">{renderBody()}</div>

      {dialogOpen && (
        <AddItemDialog
          onClose={() => setDialogOpen(false)}
          onSaved={(saved) => {
            setDialogOpen(false);
            onItemSaved(saved);
          }}
        />
      )}

      {itemToEdit && (
        <AddItemDialog
          item={itemToEdit}
          onClose={() => setItemToEdit(null)}
          onSaved={(saved) => {
            setItemToEdit(null);
            onItemUpdated(saved);
          }}
        />
      )}

      {itemToDelete && (
        <DeleteItemDialog
          item={itemToDelete}
          onClose={() => setItemToDelete(null)}
          onDeleted={(removed) => {
            setItemToDelete(null);
            onItemDeleted(removed);
          }}
        />
      )}
    </section>
  );
}
