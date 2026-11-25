import { useCallback, useEffect, useRef, useState } from 'react';
import Sidebar from './components/Sidebar.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Inventory from './pages/Inventory.jsx';
import RecipesPage from './pages/RecipesPage.jsx';
import { listItems } from './api/items.js';

const TOAST_MS = 4000;

export default function App() {
  const [page, setPage] = useState('dashboard');
  // One list for both screens, loaded from GET /api/items. Nothing here is sample data.
  const [items, setItems] = useState([]);
  const [load, setLoad] = useState({ status: 'loading', error: null, correlationId: null });
  const [toast, setToast] = useState(null);
  const latestRequest = useRef(0);

  const loadItems = useCallback(async () => {
    const requestId = ++latestRequest.current;
    // Keep the current list on screen during a refresh; only the first load shows a loading state.
    setLoad((prev) => (prev.status === 'ready' ? prev : { status: 'loading', error: null, correlationId: null }));
    try {
      const list = await listItems();
      if (requestId !== latestRequest.current) return;
      setItems(list);
      setLoad({ status: 'ready', error: null, correlationId: null });
    } catch (error) {
      if (requestId !== latestRequest.current) return;
      setLoad({
        status: 'error',
        error: error.message ?? 'Could not load items. Try again.',
        correlationId: error.correlationId ?? null,
      });
    }
  }, []);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(null), TOAST_MS);
    return () => clearTimeout(timer);
  }, [toast]);

  // REQ-019 (Decided Q-018): the dashboard reads current inventory each time it is shown,
  // so showing it re-runs the same shared load function.
  function handleNavigate(next) {
    setPage(next);
    if (next === 'dashboard') loadItems();
  }

  function handleItemSaved(saved) {
    setToast({ message: `Added ${saved.name}.` });
    loadItems();
  }

  // Edit and delete also refresh the one shared list, so the dashboard counts follow.
  function handleItemUpdated(saved) {
    setToast({ message: `Updated ${saved.name}.` });
    loadItems();
  }

  function handleItemDeleted(removed) {
    setToast({ message: `Deleted ${removed.name}.` });
    loadItems();
  }

  const shared = {
    items,
    status: load.status,
    error: load.error,
    correlationId: load.correlationId,
    onRetry: loadItems,
  };

  return (
    <div className="app">
      <Sidebar active={page} onNavigate={handleNavigate} />
      <main className="content">
        {page === 'dashboard' && <Dashboard {...shared} />}
        {page === 'recipes' && <RecipesPage />}
        {page === 'inventory' && (
          <Inventory
            {...shared}
            onItemSaved={handleItemSaved}
            onItemUpdated={handleItemUpdated}
            onItemDeleted={handleItemDeleted}
          />
        )}
      </main>
      {toast && (
        <div className="toast" role="status" aria-live="polite">
          {toast.message}
        </div>
      )}
    </div>
  );
}
