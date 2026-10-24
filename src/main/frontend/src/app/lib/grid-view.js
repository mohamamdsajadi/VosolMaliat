'use client';
import { useState, useSyncExternalStore } from 'react';

const defaults = { hidden: [], dense: true };
const cache = new Map();
const memory = new Map();
const eventName = 'grid-view-changed';
function subscribe(listener) {
  window.addEventListener('storage', listener);
  window.addEventListener(eventName, listener);
  return () => { window.removeEventListener('storage', listener); window.removeEventListener(eventName, listener); };
}
function read(key) {
  let raw = memory.get(key);
  if (!memory.has(key)) {
    try { raw = window.localStorage.getItem(key); } catch { /* Fall back to this session's preferences. */ }
  }
  if (cache.has(key) && cache.get(key).raw === raw) return cache.get(key).value;
  let value = defaults;
  try {
    const saved = JSON.parse(raw);
    if (Array.isArray(saved?.hidden)) value = { hidden: saved.hidden.filter(item => typeof item === 'string'), dense: saved.dense !== false };
  } catch { /* An unavailable or older preference uses the default columns. */ }
  cache.set(key, { raw, value });
  return value;
}

export function useGridView(grid, user, columns) {
  const key = `vosol:grid:v1:${user?.username || user?.id || 'guest'}:${grid}`;
  const value = useSyncExternalStore(subscribe, () => read(key), () => defaults);
  const validHidden = value.hidden.filter(column => columns.some(item => item.key === column));
  const hiddenColumns = validHidden.length === columns.length ? [] : validHidden;
  const save = next => {
    const raw = JSON.stringify(next);
    try { window.localStorage.setItem(key, raw); memory.delete(key); }
    catch { memory.set(key, raw); }
    cache.delete(key);
    window.dispatchEvent(new Event(eventName));
  };
  return {
    hiddenColumns, dense: value.dense,
    toggleColumn: column => {
      const hidden = hiddenColumns.includes(column) ? hiddenColumns.filter(item => item !== column) : [...hiddenColumns, column];
      if (hidden.length < columns.length) save({ ...value, hidden });
    },
    setDense: dense => save({ ...value, dense }),
    reset: () => save(defaults),
  };
}

// Selection belongs to the current result set. Paging/filtering/refresh never reuses stale IDs.
export function useGridSelection(scope, rowIds) {
  const [selection, setSelection] = useState({ scope, ids: [] });
  if (selection.scope !== scope) setSelection({ scope, ids: [] });
  const ids = new Set(selection.scope === scope ? selection.ids.filter(id => rowIds.includes(id)) : []);
  return {
    ids,
    toggle: id => setSelection(current => {
      const selected = current.scope === scope ? current.ids.filter(value => rowIds.includes(value)) : [];
      return { scope, ids: selected.includes(id) ? selected.filter(value => value !== id) : [...selected, id] };
    }),
    toggleAll: event => setSelection({ scope, ids: event.target.checked ? rowIds : [] }),
    clear: () => setSelection({ scope, ids: [] }),
  };
}
