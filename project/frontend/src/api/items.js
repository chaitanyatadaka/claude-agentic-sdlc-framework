import { apiRequest, ApiError } from './client.js';

// GET /api/items: ordered by name then id on the server. Each item carries its expiry status.
export async function listItems() {
  const data = await apiRequest('/api/items');
  if (!Array.isArray(data)) {
    throw new ApiError({ kind: 'http', message: 'Unexpected response from the server. Try again.' });
  }
  return data;
}

// POST /api/items: returns 201 with the saved item.
export function createItem(payload) {
  return apiRequest('/api/items', { method: 'POST', body: payload });
}

// PUT /api/items/{id}: same body and rules as POST. Returns 200 with the updated item; 404 if missing.
export function updateItem(id, payload) {
  return apiRequest(`/api/items/${encodeURIComponent(id)}`, { method: 'PUT', body: payload });
}

// DELETE /api/items/{id}: returns 204 with no body (apiRequest resolves to null); 404 if missing.
export function deleteItem(id) {
  return apiRequest(`/api/items/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
