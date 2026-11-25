import { defineConfig } from '@playwright/test';

// The backend (port 8080) and the Vite dev server (port 5174) are started outside this config.
// Browsers come from the installed Google Chrome channel, so no browser download is needed.
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5174',
    channel: 'chrome',
    headless: true,
  },
});
