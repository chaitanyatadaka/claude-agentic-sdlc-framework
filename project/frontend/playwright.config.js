import { defineConfig } from '@playwright/test';

// The backend (port 8080) and the Vite dev server (port 5174) are started outside this config.
// Browsers come from the installed Google Chrome channel, so no browser download is needed.
export default defineConfig({
  testDir: './e2e',
  // The specs share one backend database. Run them one at a time so that counts in one test
  // are not changed by another test's writes.
  workers: 1,
  use: {
    baseURL: 'http://localhost:5174',
    channel: 'chrome',
    headless: true,
  },
});
