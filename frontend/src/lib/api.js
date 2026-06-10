const rawBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';
const API_BASE_URL = rawBaseUrl.replace(/\/$/, '');

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || '';

  if (response.status === 204) {
    return null;
  }

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

async function buildError(response) {
  const error = new Error(`Request failed with status ${response.status}`);
  error.status = response.status;
  error.statusCode = response.status;

  try {
    const payload = await parseResponse(response);
    if (payload && typeof payload === 'object') {
      if (payload.message) error.message = payload.message;
      else if (payload.error) error.message = payload.error;
      else if (payload.detail) error.message = payload.detail;
      else error.message = JSON.stringify(payload);
      return error;
    }
    if (typeof payload === 'string' && payload.trim()) {
      error.message = payload;
      return error;
    }
  } catch {
    // Fall through to generic message.
  }

  return error;
}

function headersFor(token) {
  const headers = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function request(path, { method = 'GET', body, token, signal } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: headersFor(token),
    signal,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) {
    throw await buildError(response);
  }

  return parseResponse(response);
}

export const api = {
  login: (payload) => request('/auth/login', { method: 'POST', body: payload }),
  register: (payload) => request('/auth/register', { method: 'POST', body: payload }),
  refresh: (payload) => request('/auth/refresh', { method: 'POST', body: payload }),
  logout: (payload) => request('/auth/logout', { method: 'POST', body: payload }),
  listProjects: (token) => request('/projects', { token }),
  createProject: (token, payload) => request('/projects', { method: 'POST', body: payload, token }),
  updateProject: (token, id, payload) =>
    request(`/projects/${id}`, { method: 'PUT', body: payload, token }),
  deleteProject: (token, id) => request(`/projects/${id}`, { method: 'DELETE', token }),
  addProjectMember: (token, id, payload) =>
    request(`/projects/${id}/members`, { method: 'POST', body: payload, token }),
  listSprints: (token, projectId) => request(`/sprints?projectId=${encodeURIComponent(projectId)}`, { token }),
  createSprint: (token, payload) => request('/sprints', { method: 'POST', body: payload, token }),
  startSprint: (token, id) => request(`/sprints/${id}/start`, { method: 'POST', token }),
  completeSprint: (token, id) => request(`/sprints/${id}/complete`, { method: 'POST', token }),
  addTaskToSprint: (token, id, payload) =>
    request(`/sprints/${id}/tasks`, { method: 'POST', body: payload, token }),
  listTasks: (token, query = {}) => {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params.set(key, value);
      }
    });
    const suffix = params.toString() ? `?${params.toString()}` : '';
    return request(`/tasks${suffix}`, { token });
  },
  createTask: (token, payload) => request('/tasks', { method: 'POST', body: payload, token }),
  updateTask: (token, id, payload) => request(`/tasks/${id}`, { method: 'PUT', body: payload, token }),
  updateTaskStatus: (token, id, payload) =>
    request(`/tasks/${id}/status`, { method: 'PATCH', body: payload, token }),
  deleteTask: (token, id) => request(`/tasks/${id}`, { method: 'DELETE', token }),
};
