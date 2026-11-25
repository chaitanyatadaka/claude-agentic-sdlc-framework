import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

// Scaffold placeholder. The real screens are added by later tasks (T19 onwards).
function Scaffold() {
  return <h1>Smart Pantry</h1>;
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Scaffold />
  </StrictMode>,
);
