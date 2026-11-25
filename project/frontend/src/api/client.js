// Small fetch wrapper for the backend API (REQ-032, DR-011, DR-024).
// Paths are relative (/api), so the Vite dev proxy or the Spring Boot host serves them.
// Raw response bodies, stack traces and URLs are never exposed; only readable messages are.

export const REQUEST_TIMEOUT_MS = 10000;

export class ApiError extends Error {
  constructor({ message, kind = 'http', status = null, fieldErrors = {}, correlationId = null }) {
    super(message);
    this.name = 'ApiError';
    this.kind = kind; // 'http' | 'network' | 'timeout'
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.correlationId = correlationId;
  }
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function readableMessage(status, body) {
  // Backend 4xx messages are written for people. 5xx messages are replaced with a generic line.
  if (status < 500 && isPlainObject(body) && typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }
  if (status === 400) return 'Some details are not valid. Check the highlighted fields.';
  if (status === 404) return 'The requested item could not be found.';
  if (status >= 500) return 'Something went wrong on the server. Try again.';
  return 'The request failed. Try again.';
}

async function readBody(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null; // Non-JSON bodies (for example a proxy error page) are ignored.
  }
}

export async function apiRequest(path, { method = 'GET', body } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const headers = { Accept: 'application/json' };
  const init = { method, headers, signal: controller.signal };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }

  try {
    const response = await fetch(path, init);
    const data = await readBody(response);
    if (!response.ok) {
      throw new ApiError({
        kind: 'http',
        status: response.status,
        message: readableMessage(response.status, data),
        fieldErrors: isPlainObject(data) && isPlainObject(data.fieldErrors) ? data.fieldErrors : {},
        correlationId: isPlainObject(data) && typeof data.correlationId === 'string' ? data.correlationId : null,
      });
    }
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error?.name === 'AbortError') {
      throw new ApiError({ kind: 'timeout', message: 'The request timed out. Try again.' });
    }
    throw new ApiError({ kind: 'network', message: 'Cannot reach the server. Try again.' });
  } finally {
    clearTimeout(timer);
  }
}
