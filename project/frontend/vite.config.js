import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The production bundle is written into the backend static folder so that
// Spring Boot serves the frontend from the same origin as /api.
export default defineConfig({
  plugins: [react()],
  // Use React's automatic JSX runtime so components do not need `import React`.
  esbuild: { jsx: 'automatic' },
  // In development, /api requests are forwarded to the backend so the browser sees one origin.
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
