import { useEffect, useId, useRef, useState } from 'react';
import { createItem, updateItem } from '../api/items.js';
import { ApiError } from '../api/client.js';
import { CATEGORIES } from '../data/categories.js';
import { toCreatePayload, todayIso, validateItemForm } from '../utils/itemValidation.js';

const FIELD_ORDER = ['name', 'quantity', 'category', 'purchaseDate', 'expiryDate'];

function withoutKey(map, key) {
  const next = { ...map };
  delete next[key];
  return next;
}

// Form values for editing an existing item (the list row carries the same fields as the API).
function valuesFromItem(item) {
  return {
    name: item.name,
    quantity: String(item.quantity),
    category: item.category,
    purchaseDate: item.purchaseDate,
    expiryDate: item.expiryDate ?? '',
  };
}

// Modal dialog for POST /api/items, or for PUT /api/items/{id} when an item is passed (edit mode).
// Mounted only while open, so the form resets each time.
// onClose runs on Cancel and Escape. onSaved receives the saved item after a 201 or 200.
export default function AddItemDialog({ item = null, onClose, onSaved }) {
  const editing = item !== null;
  const dialogRef = useRef(null);
  const nameRef = useRef(null);
  const titleId = useId();
  const [values, setValues] = useState(() => (editing ? valuesFromItem(item) : {
    name: '',
    quantity: '',
    category: '',
    purchaseDate: todayIso(),
    expiryDate: '',
  }));
  const [clientErrors, setClientErrors] = useState({});
  const [serverErrors, setServerErrors] = useState({});
  const [summary, setSummary] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    // The element that opened the dialog (the Add item button) gets focus back on close.
    const opener = document.activeElement;
    if (dialog && !dialog.open) dialog.showModal();
    nameRef.current?.focus();
    return () => {
      if (dialog?.open) dialog.close();
      if (opener instanceof HTMLElement) opener.focus();
    };
  }, []);

  function fieldMessage(field) {
    return clientErrors[field] || serverErrors[field] || null;
  }

  function focusField(field) {
    document.getElementById(`item-${field}`)?.focus();
  }

  function updateField(field, value) {
    setValues((prev) => ({ ...prev, [field]: value }));
    setClientErrors((prev) => withoutKey(prev, field));
    setServerErrors((prev) => withoutKey(prev, field));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (saving) return;

    setServerErrors({});
    setSummary('');
    const errors = validateItemForm(values);
    setClientErrors(errors);

    const firstInvalid = FIELD_ORDER.find((field) => errors[field]);
    if (firstInvalid) {
      setSummary('Please fix the highlighted fields.');
      focusField(firstInvalid);
      return;
    }

    setSaving(true);
    try {
      const payload = toCreatePayload(values);
      const saved = editing ? await updateItem(item.id, payload) : await createItem(payload);
      onSaved(saved);
    } catch (error) {
      const apiError = error instanceof ApiError ? error : null;
      const fieldErrors = apiError?.fieldErrors ?? {};
      setSaving(false);
      setServerErrors(fieldErrors);
      setSummary(apiError?.message ?? 'The item could not be saved. Try again.');
      const firstServerField = FIELD_ORDER.find((field) => fieldErrors[field]);
      if (firstServerField) focusField(firstServerField);
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        // Escape: keep React in charge of closing so focus is restored consistently.
        event.preventDefault();
        onClose();
      }}
    >
      <form className="item-form" noValidate onSubmit={handleSubmit}>
        <h2 id={titleId}>{editing ? 'Edit item' : 'Add item'}</h2>

        {summary && <div className="form-summary" role="alert">{summary}</div>}

        <Field id="item-name" label="Name" error={fieldMessage('name')}>
          <input
            ref={nameRef}
            id="item-name"
            className="input"
            type="text"
            autoComplete="off"
            value={values.name}
            onChange={(e) => updateField('name', e.target.value)}
            aria-invalid={fieldMessage('name') ? 'true' : undefined}
            aria-describedby={fieldMessage('name') ? 'item-name-error' : undefined}
          />
        </Field>

        <Field id="item-quantity" label="Quantity" error={fieldMessage('quantity')}>
          <input
            id="item-quantity"
            className="input"
            type="number"
            inputMode="numeric"
            min="1"
            step="1"
            value={values.quantity}
            onChange={(e) => updateField('quantity', e.target.value)}
            aria-invalid={fieldMessage('quantity') ? 'true' : undefined}
            aria-describedby={fieldMessage('quantity') ? 'item-quantity-error' : undefined}
          />
        </Field>

        <Field id="item-category" label="Category" error={fieldMessage('category')}>
          <select
            id="item-category"
            className="input"
            value={values.category}
            onChange={(e) => updateField('category', e.target.value)}
            aria-invalid={fieldMessage('category') ? 'true' : undefined}
            aria-describedby={fieldMessage('category') ? 'item-category-error' : undefined}
          >
            <option value="">Select a category</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </Field>

        <Field id="item-purchaseDate" label="Purchase date" error={fieldMessage('purchaseDate')}>
          <input
            id="item-purchaseDate"
            className="input"
            type="date"
            value={values.purchaseDate}
            onChange={(e) => updateField('purchaseDate', e.target.value)}
            aria-invalid={fieldMessage('purchaseDate') ? 'true' : undefined}
            aria-describedby={fieldMessage('purchaseDate') ? 'item-purchaseDate-error' : undefined}
          />
        </Field>

        <Field id="item-expiryDate" label="Expiry date (optional)" error={fieldMessage('expiryDate')}>
          <input
            id="item-expiryDate"
            className="input"
            type="date"
            value={values.expiryDate}
            onChange={(e) => updateField('expiryDate', e.target.value)}
            aria-invalid={fieldMessage('expiryDate') ? 'true' : undefined}
            aria-describedby={fieldMessage('expiryDate') ? 'item-expiryDate-error' : undefined}
          />
        </Field>

        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save item'}
          </button>
        </div>
      </form>
    </dialog>
  );
}

function Field({ id, label, error, children }) {
  return (
    <div className={`field ${error ? 'has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error && <p id={`${id}-error`} className="field-error">{error}</p>}
    </div>
  );
}
