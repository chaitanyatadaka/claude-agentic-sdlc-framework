import { useEffect, useId, useRef, useState } from 'react';
import { deleteItem } from '../api/items.js';
import { ApiError } from '../api/client.js';

// Confirmation dialog for DELETE /api/items/{id}. Cancel is focused by default.
// Escape and Cancel close it without deleting. onDeleted receives the item after a 204.
// On failure the readable message is shown here and the item stays in the list.
export default function DeleteItemDialog({ item, onClose, onDeleted }) {
  const dialogRef = useRef(null);
  const cancelRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const dialog = dialogRef.current;
    // The element that opened the dialog (the row's Delete button) gets focus back on close.
    const opener = document.activeElement;
    if (dialog && !dialog.open) dialog.showModal();
    cancelRef.current?.focus();
    return () => {
      if (dialog?.open) dialog.close();
      if (opener instanceof HTMLElement) opener.focus();
    };
  }, []);

  async function handleDelete() {
    if (deleting) return;
    setDeleting(true);
    setError('');
    try {
      await deleteItem(item.id);
      onDeleted(item);
    } catch (err) {
      setDeleting(false);
      setError(err instanceof ApiError ? err.message : 'The item could not be deleted. Try again.');
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="dialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={(event) => {
        // Escape: keep React in charge of closing so focus is restored consistently.
        event.preventDefault();
        if (!deleting) onClose();
      }}
    >
      <div className="item-form">
        <h2 id={titleId}>Delete {item.name}?</h2>
        <p id={descriptionId} className="muted">
          This removes the item from your pantry. It cannot be undone.
        </p>

        {error && <div className="form-summary" role="alert">{error}</div>}

        <div className="dialog-actions">
          <button ref={cancelRef} type="button" className="btn btn-ghost" onClick={onClose} disabled={deleting}>
            Cancel
          </button>
          <button type="button" className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>
    </dialog>
  );
}
