const SESSION_KEY = 'jirops.session';
const SESSION_CHANGE_EVENT = 'jirops-session-changed';

export function loadSession() {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveSession(session, { notify = false } = {}) {
  if (typeof window === 'undefined') {
    return;
  }

  if (!session) {
    window.localStorage.removeItem(SESSION_KEY);
    if (notify) {
      window.dispatchEvent(new CustomEvent(SESSION_CHANGE_EVENT, { detail: null }));
    }
    return;
  }

  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  if (notify) {
    window.dispatchEvent(new CustomEvent(SESSION_CHANGE_EVENT, { detail: session }));
  }
}

export function onSessionChange(handler) {
  if (typeof window === 'undefined') {
    return () => {};
  }

  const listener = (event) => handler(event.detail);
  window.addEventListener(SESSION_CHANGE_EVENT, listener);
  return () => window.removeEventListener(SESSION_CHANGE_EVENT, listener);
}
