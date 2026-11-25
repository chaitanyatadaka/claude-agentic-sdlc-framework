import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The production bundle is written into the backend static folder so that
// Spring Boot serves the frontend from the same origin as /api.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
