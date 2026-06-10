import { useEffect, useMemo, useState } from 'react';
import { api } from './lib/api';
import { loadSession, saveSession } from './lib/storage';

const statusOrder = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
const statusLabels = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  DONE: 'Done',
};
const statusTone = {
  TODO: 'border-slate-500/30 bg-slate-500/10 text-slate-200',
  IN_PROGRESS: 'border-sky-500/30 bg-sky-500/10 text-sky-200',
  IN_REVIEW: 'border-amber-500/30 bg-amber-500/10 text-amber-200',
  DONE: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200',
};
const priorityTone = {
  LOW: 'text-slate-300',
  MEDIUM: 'text-cyan-200',
  HIGH: 'text-amber-200',
  CRITICAL: 'text-rose-200',
};

const emptyAuth = { email: '', password: '' };

const emptyProject = { name: '', key: '', description: '' };
const emptySprint = { name: '', goal: '', startDate: '', endDate: '' };
const emptyTask = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  sprintId: '',
  assignee: '',
  labels: '',
};

function formatDate(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Not set';
  return new Intl.DateTimeFormat('en', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date);
}

function formatDateTime(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Not set';
  return new Intl.DateTimeFormat('en', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function splitLabels(raw) {
  return raw
    .split(',')
    .map((label) => label.trim())
    .filter(Boolean);
}

function suggestProjectKey(name) {
  return name
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 10);
}

function getTaskCounts(tasks) {
  return statusOrder.reduce((acc, status) => {
    acc[status] = tasks.filter((task) => task.status === status).length;
    return acc;
  }, {});
}

function SectionCard({ title, eyebrow, action, children, className = '' }) {
  return (
    <section className={`glass rounded-3xl p-5 shadow-glow ${className}`}>
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          {eyebrow ? (
            <p className="text-xs uppercase tracking-[0.35em] text-sky-200/70">{eyebrow}</p>
          ) : null}
          <h2 className="mt-1 font-display text-xl font-semibold text-white">{title}</h2>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function Field({ label, hint, children, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-sm font-medium text-slate-200">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-xs text-slate-400">{hint}</span> : null}
    </label>
  );
}

function Input(props) {
  return (
    <input
      {...props}
      className={`w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400/40 focus:bg-white/10 ${props.className || ''}`}
    />
  );
}

function TextArea(props) {
  return (
    <textarea
      {...props}
      className={`w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-sky-400/40 focus:bg-white/10 ${props.className || ''}`}
    />
  );
}

function Select(props) {
  return (
    <select
      {...props}
      className={`w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400/40 focus:bg-white/10 ${props.className || ''}`}
    />
  );
}

function Button({ variant = 'primary', className = '', ...props }) {
  const styles = {
    primary:
      'bg-gradient-to-r from-sky-500 to-cyan-400 text-slate-950 shadow-[0_18px_40px_rgba(56,189,248,0.24)] hover:brightness-110',
    secondary: 'border border-white/10 bg-white/5 text-white hover:bg-white/10',
    ghost: 'text-slate-300 hover:bg-white/5 hover:text-white',
    danger: 'border border-rose-500/20 bg-rose-500/10 text-rose-100 hover:bg-rose-500/20',
  };

  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center rounded-2xl px-4 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${styles[variant]} ${className}`}
    />
  );
}

function Badge({ children, className = '' }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border border-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] ${className}`}
    >
      {children}
    </span>
  );
}

function StatCard({ label, value, detail }) {
  return (
    <div className="glass rounded-3xl p-4 shadow-glow">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-400">{label}</p>
      <div className="mt-3 flex items-end justify-between gap-3">
        <div className="text-3xl font-bold text-white">{value}</div>
        {detail ? <div className="text-sm text-slate-300">{detail}</div> : null}
      </div>
    </div>
  );
}

function App() {
  const [session, setSession] = useState(() => loadSession() || null);
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState(emptyAuth);
  const [projects, setProjects] = useState([]);
  const [sprints, setSprints] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [projectForm, setProjectForm] = useState(emptyProject);
  const [sprintForm, setSprintForm] = useState(emptySprint);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [projectMemberId, setProjectMemberId] = useState('');
  const [taskFilter, setTaskFilter] = useState('');
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState({
    auth: false,
    projects: false,
    sprints: false,
    tasks: false,
    projectCreate: false,
    sprintCreate: false,
    taskCreate: false,
  });

  const activeProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId) || null,
    [projects, selectedProjectId],
  );

  const taskCounts = useMemo(() => getTaskCounts(tasks), [tasks]);
  const activeSprints = useMemo(
    () => sprints.filter((sprint) => sprint.status === 'ACTIVE'),
    [sprints],
  );

  useEffect(() => {
    saveSession(session);
  }, [session]);

  useEffect(() => {
    if (!projects.length) {
      setSelectedProjectId('');
      return;
    }

    if (!selectedProjectId || !projects.some((project) => project.id === selectedProjectId)) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  useEffect(() => {
    if (!selectedProjectId || !session?.accessToken) {
      setSprints([]);
      setTasks([]);
      return;
    }

    let cancelled = false;

    async function loadWorkspace() {
      setBusy((current) => ({ ...current, sprints: true, tasks: true }));
      setMessage(null);

      try {
        const [nextSprints, nextTasks] = await Promise.all([
          api.listSprints(session.accessToken, selectedProjectId),
          api.listTasks(session.accessToken, {
            projectId: selectedProjectId,
            status: taskFilter || undefined,
          }),
        ]);

        if (!cancelled) {
          setSprints(Array.isArray(nextSprints) ? nextSprints : []);
          setTasks(Array.isArray(nextTasks) ? nextTasks : []);
        }
      } catch (error) {
        if (!cancelled) {
          setMessage({ type: 'error', text: error.message });
        }
      } finally {
        if (!cancelled) {
          setBusy((current) => ({ ...current, sprints: false, tasks: false }));
        }
      }
    }

    loadWorkspace();

    return () => {
      cancelled = true;
    };
  }, [selectedProjectId, session?.accessToken, taskFilter]);

  useEffect(() => {
    if (!session?.accessToken) {
      setProjects([]);
      return;
    }

    let cancelled = false;

    async function loadProjects() {
      setBusy((current) => ({ ...current, projects: true }));
      setMessage(null);

      try {
        const nextProjects = await api.listProjects(session.accessToken);
        if (!cancelled) {
          setProjects(Array.isArray(nextProjects) ? nextProjects : []);
        }
      } catch (error) {
        if (!cancelled) {
          setMessage({ type: 'error', text: error.message });
        }
      } finally {
        if (!cancelled) {
          setBusy((current) => ({ ...current, projects: false }));
        }
      }
    }

    loadProjects();

    return () => {
      cancelled = true;
    };
  }, [session?.accessToken]);

  useEffect(() => {
    setProjectMemberId('');
    setSprintForm(emptySprint);
    setTaskForm(emptyTask);
  }, [selectedProjectId]);

  function notify(type, text) {
    setMessage({ type, text });
  }

  function requireToken() {
    if (!session?.accessToken) {
      throw new Error('Sign in first');
    }
    return session.accessToken;
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setBusy((current) => ({ ...current, auth: true }));
    setMessage(null);

    try {
      const payload = {
        email: authForm.email.trim(),
        password: authForm.password,
      };
      const response =
        authMode === 'login' ? await api.login(payload) : await api.register(payload);

      setSession({
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });
      setAuthForm(emptyAuth);
      notify('success', authMode === 'login' ? 'Welcome back.' : 'Account created.');
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy((current) => ({ ...current, auth: false }));
    }
  }

  async function handleLogout() {
    if (!session?.refreshToken) {
      setSession(null);
      setProjects([]);
      setSprints([]);
      setTasks([]);
      return;
    }

    try {
      await api.logout({ refreshToken: session.refreshToken });
    } catch {
      // Best effort logout.
    } finally {
      setSession(null);
      setProjects([]);
      setSprints([]);
      setTasks([]);
      setSelectedProjectId('');
      notify('info', 'Signed out.');
    }
  }

  async function refreshProjects(nextSelectedId) {
    const token = requireToken();
    const nextProjects = await api.listProjects(token);
    setProjects(Array.isArray(nextProjects) ? nextProjects : []);
    if (nextSelectedId) {
      setSelectedProjectId(nextSelectedId);
    }
  }

  async function refreshWorkspace(projectId = selectedProjectId) {
    const token = requireToken();
    const [nextSprints, nextTasks] = await Promise.all([
      api.listSprints(token, projectId),
      api.listTasks(token, { projectId, status: taskFilter || undefined }),
    ]);
    setSprints(Array.isArray(nextSprints) ? nextSprints : []);
    setTasks(Array.isArray(nextTasks) ? nextTasks : []);
  }

  async function handleProjectSubmit(event) {
    event.preventDefault();
    setBusy((current) => ({ ...current, projectCreate: true }));
    setMessage(null);

    try {
      const token = requireToken();
      const response = await api.createProject(token, {
        name: projectForm.name.trim(),
        key: projectForm.key.trim().toUpperCase(),
        description: projectForm.description.trim() || null,
      });

      await refreshProjects(response.id);
      setProjectForm(emptyProject);
      notify('success', `Project ${response.key} created.`);
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy((current) => ({ ...current, projectCreate: false }));
    }
  }

  async function handleProjectUpdate(project) {
    const nextName = window.prompt('Project name', project.name);
    if (nextName === null) return;

    const nextDescription = window.prompt('Project description', project.description || '');
    if (nextDescription === null) return;

    try {
      const token = requireToken();
      await api.updateProject(token, project.id, {
        name: nextName.trim(),
        description: nextDescription.trim(),
      });
      await refreshProjects(project.id);
      await refreshWorkspace(project.id);
      notify('success', 'Project updated.');
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleProjectDelete(project) {
    const confirmed = window.confirm(`Delete ${project.name}?`);
    if (!confirmed) return;

    try {
      const token = requireToken();
      await api.deleteProject(token, project.id);
      const nextProjects = projects.filter((item) => item.id !== project.id);
      setProjects(nextProjects);
      const fallbackId = nextProjects[0]?.id || '';
      setSelectedProjectId(fallbackId);
      if (fallbackId) {
        await refreshWorkspace(fallbackId);
      } else {
        setSprints([]);
        setTasks([]);
      }
      notify('success', 'Project deleted.');
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleProjectMemberSubmit(event, projectId) {
    event.preventDefault();
    if (!projectId) {
      return;
    }
    const data = new FormData(event.currentTarget);
    const userId = String(data.get('userId') || '').trim();
    if (!userId) return;

    try {
      const token = requireToken();
      await api.addProjectMember(token, projectId, { userId });
      await refreshProjects(projectId);
      notify('success', 'Member added.');
      setProjectMemberId('');
      event.currentTarget.reset();
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleSprintSubmit(event) {
    event.preventDefault();
    if (!selectedProjectId) {
      return;
    }
    setBusy((current) => ({ ...current, sprintCreate: true }));
    setMessage(null);

    try {
      const token = requireToken();
      const response = await api.createSprint(token, {
        projectId: selectedProjectId,
        name: sprintForm.name.trim(),
        goal: sprintForm.goal.trim() || null,
        startDate: sprintForm.startDate || null,
        endDate: sprintForm.endDate || null,
      });

      setSprintForm(emptySprint);
      await refreshWorkspace(selectedProjectId);
      notify('success', `Sprint ${response.name} created.`);
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy((current) => ({ ...current, sprintCreate: false }));
    }
  }

  async function handleSprintAction(action, sprint) {
    try {
      const token = requireToken();
      await action(token, sprint.id);
      await refreshWorkspace(selectedProjectId);
      notify('success', `Sprint ${sprint.name} updated.`);
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleAddTaskToSprint(event, sprintId) {
    event.preventDefault();
    if (!selectedProjectId || !sprintId) {
      return;
    }
    const data = new FormData(event.currentTarget);
    const taskId = String(data.get('taskId') || '').trim();
    if (!taskId) return;

    try {
      const token = requireToken();
      await api.addTaskToSprint(token, sprintId, { taskId });
      await refreshWorkspace(selectedProjectId);
      notify('success', 'Task added to sprint.');
      event.currentTarget.reset();
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleTaskSubmit(event) {
    event.preventDefault();
    if (!selectedProjectId) {
      return;
    }
    setBusy((current) => ({ ...current, taskCreate: true }));
    setMessage(null);

    try {
      const token = requireToken();
      await api.createTask(token, {
        projectId: selectedProjectId,
        sprintId: taskForm.sprintId || null,
        title: taskForm.title.trim(),
        description: taskForm.description.trim() || null,
        priority: taskForm.priority || null,
        assignee: taskForm.assignee.trim() || null,
        labels: splitLabels(taskForm.labels),
      });

      setTaskForm({ ...emptyTask, sprintId: taskForm.sprintId });
      await refreshWorkspace(selectedProjectId);
      notify('success', 'Task created.');
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy((current) => ({ ...current, taskCreate: false }));
    }
  }

  async function handleTaskStatusChange(task, status) {
    try {
      const token = requireToken();
      await api.updateTaskStatus(token, task.id, { status });
      await refreshWorkspace(selectedProjectId);
      notify('success', `Task moved to ${statusLabels[status]}.`);
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleTaskDelete(task) {
    const confirmed = window.confirm(`Delete task "${task.title}"?`);
    if (!confirmed) return;

    try {
      const token = requireToken();
      await api.deleteTask(token, task.id);
      await refreshWorkspace(selectedProjectId);
      notify('success', 'Task deleted.');
    } catch (error) {
      notify('error', error.message);
    }
  }

  const isAuthenticated = Boolean(session?.accessToken);

  return (
    <div className="noise relative min-h-screen overflow-hidden">
      <div className="absolute -left-32 top-24 h-80 w-80 rounded-full bg-sky-500/18 blur-3xl" />
      <div className="absolute right-0 top-20 h-[28rem] w-[28rem] rounded-full bg-cyan-400/10 blur-3xl" />
      <div className="relative mx-auto flex min-h-screen max-w-[1600px] flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="glass mb-4 rounded-[2rem] px-5 py-4 shadow-glow">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.4em] text-sky-200/70">Jirops</p>
              <div className="mt-1 flex flex-wrap items-center gap-3">
                <h1 className="font-display text-3xl font-bold text-white">Workspace control room</h1>
                <Badge className="border-cyan-400/20 bg-cyan-400/10 text-cyan-100">
                  React + Tailwind
                </Badge>
              </div>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-300">
                A focused dashboard for projects, sprints, and tasks. It uses the backend through
                the API gateway and keeps the interaction model compact enough for daily work.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {isAuthenticated ? (
                <>
                  <Badge className="border-emerald-400/20 bg-emerald-400/10 text-emerald-100">
                    Connected
                  </Badge>
                  <Button variant="secondary" onClick={handleLogout}>
                    Sign out
                  </Button>
                </>
              ) : (
                <Badge className="border-amber-400/20 bg-amber-400/10 text-amber-100">
                  Sign in to continue
                </Badge>
              )}
            </div>
          </div>
        </header>

        {message ? (
          <div
            className={`mb-4 rounded-2xl border px-4 py-3 text-sm ${
              message.type === 'error'
                ? 'border-rose-500/20 bg-rose-500/10 text-rose-100'
                : message.type === 'success'
                  ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-100'
                  : 'border-slate-500/20 bg-slate-500/10 text-slate-100'
            }`}
          >
            {message.text}
          </div>
        ) : null}

        {!isAuthenticated ? (
          <div className="grid flex-1 gap-6 lg:grid-cols-[1.15fr_0.85fr]">
            <section className="glass relative overflow-hidden rounded-[2rem] p-8 shadow-glow">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(56,189,248,0.12),transparent_30%),radial-gradient(circle_at_bottom_left,rgba(16,185,129,0.08),transparent_25%)]" />
              <div className="relative max-w-2xl">
                <Badge className="border-sky-400/20 bg-sky-400/10 text-sky-100">Team hub</Badge>
                <h2 className="mt-5 font-display text-5xl font-bold leading-tight text-white">
                  Build a calm command center for product execution.
                </h2>
                <p className="mt-5 max-w-xl text-base leading-7 text-slate-300">
                  Projects, sprints, and tasks are presented with a deliberate visual hierarchy:
                  quick access, useful summaries, and action-heavy cards that stay readable on
                  desktop and mobile.
                </p>

                <div className="mt-8 grid gap-4 sm:grid-cols-3">
                  <StatCard label="Layers" value="3" detail="Projects, sprints, tasks" />
                  <StatCard label="Flow" value="Fast" detail="API-connected UX" />
                  <StatCard label="Style" value="Bold" detail="Glass and gradients" />
                </div>
              </div>
            </section>

            <section className="glass rounded-[2rem] p-6 shadow-glow">
              <div className="mb-6 flex rounded-2xl border border-white/10 bg-white/5 p-1">
                <button
                  className={`flex-1 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                    authMode === 'login' ? 'bg-white/10 text-white' : 'text-slate-400'
                  }`}
                  onClick={() => setAuthMode('login')}
                  type="button"
                >
                  Login
                </button>
                <button
                  className={`flex-1 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                    authMode === 'register' ? 'bg-white/10 text-white' : 'text-slate-400'
                  }`}
                  onClick={() => setAuthMode('register')}
                  type="button"
                >
                  Register
                </button>
              </div>

              <form className="space-y-4" onSubmit={handleAuthSubmit}>
                <Field label="Email">
                  <Input
                    type="email"
                    autoComplete="email"
                    value={authForm.email}
                    onChange={(event) =>
                      setAuthForm((current) => ({ ...current, email: event.target.value }))
                    }
                    placeholder="you@company.com"
                  />
                </Field>

                <Field
                  label="Password"
                  hint={authMode === 'register' ? 'Minimum 8 characters.' : undefined}
                >
                  <Input
                    type="password"
                    autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                    value={authForm.password}
                    onChange={(event) =>
                      setAuthForm((current) => ({ ...current, password: event.target.value }))
                    }
                    placeholder="••••••••"
                  />
                </Field>

                <Button type="submit" disabled={busy.auth} className="w-full">
                  {busy.auth
                    ? 'Working...'
                    : authMode === 'login'
                      ? 'Sign in to dashboard'
                      : 'Create account'}
                </Button>
              </form>
            </section>
          </div>
        ) : (
          <div className="grid flex-1 gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
            <aside className="space-y-4">
              <SectionCard title="Projects" eyebrow="Workspace" className="sticky top-4">
                <div className="space-y-3">
                  <div className="grid gap-2">
                    <Button
                      variant="secondary"
                      className="justify-start"
                      onClick={() => {
                        setSelectedProjectId('');
                        setSprints([]);
                        setTasks([]);
                      }}
                      type="button"
                    >
                      All projects
                    </Button>
                    {projects.map((project) => (
                      <button
                        key={project.id}
                        type="button"
                        onClick={() => setSelectedProjectId(project.id)}
                        className={`rounded-2xl border px-4 py-3 text-left transition ${
                          selectedProjectId === project.id
                            ? 'border-sky-400/30 bg-sky-500/15 text-white shadow-[0_16px_50px_rgba(14,165,233,0.15)]'
                            : 'border-white/10 bg-white/5 text-slate-200 hover:bg-white/10'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <div className="font-semibold">{project.name}</div>
                            <div className="text-xs uppercase tracking-[0.22em] text-slate-400">
                              {project.key}
                            </div>
                          </div>
                          <Badge className="border-white/10 bg-white/5 text-slate-200">
                            {project.members?.length || 0}
                          </Badge>
                        </div>
                      </button>
                    ))}
                    {busy.projects ? <p className="text-sm text-slate-400">Loading projects...</p> : null}
                  </div>

                  <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
                    <p className="mb-3 text-xs uppercase tracking-[0.28em] text-slate-400">
                      Quick member add
                    </p>
                    <form
                      className="space-y-2"
                      onSubmit={(event) => handleProjectMemberSubmit(event, selectedProjectId)}
                    >
                      <Input
                        name="userId"
                        placeholder="User UUID"
                        value={projectMemberId}
                        onChange={(event) => setProjectMemberId(event.target.value)}
                      />
                      <Button type="submit" variant="secondary" className="w-full" disabled={!selectedProjectId}>
                        Add member
                      </Button>
                    </form>
                  </div>
                </div>
              </SectionCard>
            </aside>

            <main className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                <StatCard label="Projects" value={projects.length} detail="Total workspaces" />
                <StatCard label="Active sprints" value={activeSprints.length} detail="Currently running" />
                <StatCard
                  label="Tasks"
                  value={tasks.length}
                  detail={`${taskCounts.TODO} todo, ${taskCounts.DONE} done`}
                />
                <StatCard
                  label="Selected"
                  value={activeProject ? activeProject.key : 'None'}
                  detail={activeProject ? activeProject.name : 'Choose a project'}
                />
              </div>

              <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
                <SectionCard
                  title="Project studio"
                  eyebrow="Create and manage"
                  action={
                    <Badge className="border-sky-400/20 bg-sky-400/10 text-sky-100">
                      {activeProject ? activeProject.key : 'No project'}
                    </Badge>
                  }
                >
                  <div className="grid gap-4 lg:grid-cols-2">
                    <form className="space-y-4" onSubmit={handleProjectSubmit}>
                      <div className="grid gap-4 sm:grid-cols-2">
                        <Field label="Name">
                          <Input
                            value={projectForm.name}
                            onChange={(event) =>
                              setProjectForm((current) => {
                                const nextName = event.target.value;
                                return {
                                  ...current,
                                  name: nextName,
                                  key: current.key ? current.key : suggestProjectKey(nextName),
                                };
                              })
                            }
                            placeholder="Launch pad"
                          />
                        </Field>
                        <Field label="Key" hint="Uppercase letters and digits only">
                          <Input
                            value={projectForm.key}
                            onChange={(event) =>
                              setProjectForm((current) => ({
                                ...current,
                                key: event.target.value.toUpperCase(),
                              }))
                            }
                            placeholder="LP"
                          />
                        </Field>
                      </div>

                      <Field label="Description">
                        <TextArea
                          rows={4}
                          value={projectForm.description}
                          onChange={(event) =>
                            setProjectForm((current) => ({ ...current, description: event.target.value }))
                          }
                          placeholder="Short description of the project."
                        />
                      </Field>

                      <Button type="submit" disabled={busy.projectCreate}>
                        {busy.projectCreate ? 'Creating...' : 'Create project'}
                      </Button>
                    </form>

                    <div className="space-y-3">
                      <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
                        <p className="text-xs uppercase tracking-[0.28em] text-slate-400">
                          Active project
                        </p>
                        {activeProject ? (
                          <div className="mt-3 space-y-3">
                            <div className="flex items-center justify-between gap-3">
                              <div>
                                <h3 className="font-display text-2xl font-semibold text-white">
                                  {activeProject.name}
                                </h3>
                                <p className="mt-1 text-sm text-slate-300">{activeProject.description}</p>
                              </div>
                              <Badge className="border-slate-500/20 bg-slate-500/10 text-slate-200">
                                {activeProject.key}
                              </Badge>
                            </div>

                            <div className="grid gap-2 text-sm text-slate-300">
                              <div className="flex items-center justify-between gap-3">
                                <span>Owner</span>
                                <span className="break-all text-right">{activeProject.ownerId}</span>
                              </div>
                              <div className="flex items-center justify-between gap-3">
                                <span>Members</span>
                                <span>{activeProject.members?.length || 0}</span>
                              </div>
                              <div className="flex items-center justify-between gap-3">
                                <span>Created</span>
                                <span>{formatDateTime(activeProject.createdAt)}</span>
                              </div>
                            </div>

                            <div className="flex flex-wrap gap-2">
                              <Button variant="secondary" type="button" onClick={() => handleProjectUpdate(activeProject)}>
                                Edit
                              </Button>
                              <Button variant="danger" type="button" onClick={() => handleProjectDelete(activeProject)}>
                                Delete
                              </Button>
                            </div>
                          </div>
                        ) : (
                          <p className="mt-3 text-sm text-slate-400">
                            Select a project from the sidebar to inspect its details, sprints, and tasks.
                          </p>
                        )}
                      </div>

                      <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
                        <p className="text-xs uppercase tracking-[0.28em] text-slate-400">
                          Projects overview
                        </p>
                        <div className="mt-3 space-y-2">
                          {projects.slice(0, 4).map((project) => (
                            <div
                              key={project.id}
                              className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-slate-950/30 px-3 py-2"
                            >
                              <div>
                                <p className="font-medium text-white">{project.name}</p>
                                <p className="text-xs text-slate-400">{project.key}</p>
                              </div>
                              <span className="text-xs text-slate-300">{project.members?.length || 0} members</span>
                            </div>
                          ))}
                          {!projects.length ? <p className="text-sm text-slate-400">No projects yet.</p> : null}
                        </div>
                      </div>
                    </div>
                  </div>
                </SectionCard>

                <SectionCard title="Sprint planner" eyebrow="Delivery rhythm">
                  <div className="space-y-4">
                    <form className="space-y-4" onSubmit={handleSprintSubmit}>
                      <div className="grid gap-4 sm:grid-cols-2">
                        <Field label="Sprint name">
                          <Input
                            value={sprintForm.name}
                            onChange={(event) =>
                              setSprintForm((current) => ({ ...current, name: event.target.value }))
                            }
                            placeholder="Sprint 12"
                            disabled={!selectedProjectId}
                          />
                        </Field>
                        <Field label="Goal">
                          <Input
                            value={sprintForm.goal}
                            onChange={(event) =>
                              setSprintForm((current) => ({ ...current, goal: event.target.value }))
                            }
                            placeholder="Ship onboarding"
                            disabled={!selectedProjectId}
                          />
                        </Field>
                      </div>

                      <div className="grid gap-4 sm:grid-cols-2">
                        <Field label="Start date">
                          <Input
                            type="date"
                            value={sprintForm.startDate}
                            onChange={(event) =>
                              setSprintForm((current) => ({ ...current, startDate: event.target.value }))
                            }
                            disabled={!selectedProjectId}
                          />
                        </Field>
                        <Field label="End date">
                          <Input
                            type="date"
                            value={sprintForm.endDate}
                            onChange={(event) =>
                              setSprintForm((current) => ({ ...current, endDate: event.target.value }))
                            }
                            disabled={!selectedProjectId}
                          />
                        </Field>
                      </div>

                      <Button type="submit" disabled={!selectedProjectId || busy.sprintCreate}>
                        {busy.sprintCreate ? 'Planning...' : 'Create sprint'}
                      </Button>
                    </form>

                    <div className="space-y-3">
                      {sprints.length ? (
                        sprints.map((sprint) => (
                          <div
                            key={sprint.id}
                            className="rounded-3xl border border-white/10 bg-white/5 p-4"
                          >
                            <div className="flex flex-wrap items-start justify-between gap-3">
                              <div>
                                <p className="font-display text-xl font-semibold text-white">{sprint.name}</p>
                                <p className="mt-1 text-sm text-slate-300">{sprint.goal || 'No goal set.'}</p>
                              </div>
                              <Badge className={statusTone[sprint.status] || 'border-white/10 bg-white/5 text-slate-200'}>
                                {sprint.status}
                              </Badge>
                            </div>

                            <div className="mt-4 grid gap-2 text-sm text-slate-300">
                              <div className="flex items-center justify-between gap-3">
                                <span>Dates</span>
                                <span>
                                  {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}
                                </span>
                              </div>
                              <div className="flex items-center justify-between gap-3">
                                <span>Tasks</span>
                                <span>{sprint.taskIds?.length || 0}</span>
                              </div>
                            </div>

                            <div className="mt-4 flex flex-wrap gap-2">
                              {sprint.status !== 'ACTIVE' ? (
                                <Button variant="secondary" type="button" onClick={() => handleSprintAction(api.startSprint, sprint)}>
                                  Start
                                </Button>
                              ) : null}
                              {sprint.status !== 'CLOSED' ? (
                                <Button
                                  variant="secondary"
                                  type="button"
                                  onClick={() => handleSprintAction(api.completeSprint, sprint)}
                                >
                                  Complete
                                </Button>
                              ) : null}
                            </div>

                            <form
                              className="mt-4 space-y-2 rounded-2xl border border-white/10 bg-slate-950/30 p-3"
                              onSubmit={(event) => handleAddTaskToSprint(event, sprint.id)}
                            >
                              <p className="text-xs uppercase tracking-[0.25em] text-slate-400">
                                Add task to sprint
                              </p>
                              <div className="flex gap-2">
                                <Select name="taskId" defaultValue="">
                                  <option value="">Choose task</option>
                                  {tasks
                                    .filter((task) => !task.sprintId || task.sprintId === sprint.id)
                                    .map((task) => (
                                      <option key={task.id} value={task.id}>
                                        {task.title}
                                      </option>
                                    ))}
                                </Select>
                                <Button type="submit" variant="secondary">
                                  Add
                                </Button>
                              </div>
                            </form>
                          </div>
                        ))
                      ) : (
                        <div className="rounded-3xl border border-dashed border-white/10 bg-white/5 p-6 text-sm text-slate-400">
                          No sprints yet for this project.
                        </div>
                      )}
                    </div>
                  </div>
                </SectionCard>
              </div>

              <SectionCard
                title="Task board"
                eyebrow="Delivery surface"
                action={
                  <div className="flex flex-wrap items-center gap-2">
                    <Select
                      value={taskFilter}
                      onChange={(event) => setTaskFilter(event.target.value)}
                      className="min-w-[12rem]"
                    >
                      <option value="">All statuses</option>
                      {statusOrder.map((status) => (
                        <option key={status} value={status}>
                          {statusLabels[status]}
                        </option>
                      ))}
                    </Select>
                    {busy.tasks ? <Badge className="border-white/10 bg-white/5 text-slate-200">Refreshing</Badge> : null}
                  </div>
                }
              >
                <div className="grid gap-4 xl:grid-cols-4">
                  {statusOrder.map((status) => {
                    const columnTasks = tasks.filter((task) => task.status === status);

                    return (
                      <div key={status} className="rounded-[1.75rem] border border-white/10 bg-white/5 p-3">
                        <div className="flex items-center justify-between gap-3 px-1 pb-3">
                          <div>
                            <p className="font-display text-lg font-semibold text-white">
                              {statusLabels[status]}
                            </p>
                            <p className="text-xs uppercase tracking-[0.22em] text-slate-400">
                              {columnTasks.length} items
                            </p>
                          </div>
                          <Badge className={statusTone[status]}>{status}</Badge>
                        </div>

                        <div className="space-y-3">
                          {columnTasks.length ? (
                            columnTasks.map((task) => (
                              <article
                                key={task.id}
                                className="rounded-3xl border border-white/10 bg-slate-950/40 p-4 shadow-[0_12px_50px_rgba(0,0,0,0.24)]"
                              >
                                <div className="flex items-start justify-between gap-3">
                                  <div>
                                    <h3 className="font-semibold text-white">{task.title}</h3>
                                    <p className="mt-1 text-sm leading-6 text-slate-300">
                                      {task.description || 'No description.'}
                                    </p>
                                  </div>
                                  <span className={`text-xs font-bold uppercase tracking-[0.2em] ${priorityTone[task.priority] || 'text-slate-300'}`}>
                                    {task.priority}
                                  </span>
                                </div>

                                <div className="mt-4 flex flex-wrap gap-2">
                                  {task.labels?.map((label) => (
                                    <Badge key={label} className="border-white/10 bg-white/5 text-slate-200">
                                      {label}
                                    </Badge>
                                  ))}
                                </div>

                                <div className="mt-4 grid gap-2 text-xs text-slate-400">
                                  <div className="flex items-center justify-between gap-3">
                                    <span>Assignee</span>
                                    <span className="break-all text-right text-slate-200">
                                      {task.assignee || 'Unassigned'}
                                    </span>
                                  </div>
                                  <div className="flex items-center justify-between gap-3">
                                    <span>Reporter</span>
                                    <span className="break-all text-right text-slate-200">
                                      {task.reporter || 'Unknown'}
                                    </span>
                                  </div>
                                  <div className="flex items-center justify-between gap-3">
                                    <span>Comments</span>
                                    <span>{task.comments?.length || 0}</span>
                                  </div>
                                  <div className="flex items-center justify-between gap-3">
                                    <span>Attachments</span>
                                    <span>{task.attachments?.length || 0}</span>
                                  </div>
                                </div>

                                <div className="mt-4 space-y-2">
                                  <Field label="Move status">
                                    <Select
                                      value={task.status}
                                      onChange={(event) => handleTaskStatusChange(task, event.target.value)}
                                    >
                                      {statusOrder.map((option) => (
                                        <option key={option} value={option}>
                                          {statusLabels[option]}
                                        </option>
                                      ))}
                                    </Select>
                                  </Field>

                                  <div className="flex gap-2">
                                    <Button
                                      variant="secondary"
                                      type="button"
                                      onClick={() => handleTaskDelete(task)}
                                      className="flex-1"
                                    >
                                      Delete
                                    </Button>
                                  </div>
                                </div>
                              </article>
                            ))
                          ) : (
                            <div className="rounded-3xl border border-dashed border-white/10 bg-slate-950/20 p-5 text-sm text-slate-400">
                              Nothing here yet.
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </SectionCard>

              <SectionCard title="Task composer" eyebrow="Create and attach">
                <form className="grid gap-4 xl:grid-cols-2" onSubmit={handleTaskSubmit}>
                  <div className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field label="Title">
                        <Input
                          value={taskForm.title}
                          onChange={(event) =>
                            setTaskForm((current) => ({ ...current, title: event.target.value }))
                          }
                          placeholder="Finalize release notes"
                          disabled={!selectedProjectId}
                        />
                      </Field>
                      <Field label="Priority">
                        <Select
                          value={taskForm.priority}
                          onChange={(event) =>
                            setTaskForm((current) => ({ ...current, priority: event.target.value }))
                          }
                          disabled={!selectedProjectId}
                        >
                          {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((priority) => (
                            <option key={priority} value={priority}>
                              {priority}
                            </option>
                          ))}
                        </Select>
                      </Field>
                    </div>

                    <Field label="Description">
                      <TextArea
                        rows={5}
                        value={taskForm.description}
                        onChange={(event) =>
                          setTaskForm((current) => ({ ...current, description: event.target.value }))
                        }
                        placeholder="What needs to be done?"
                        disabled={!selectedProjectId}
                      />
                    </Field>
                  </div>

                  <div className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field label="Sprint">
                        <Select
                          value={taskForm.sprintId}
                          onChange={(event) =>
                            setTaskForm((current) => ({ ...current, sprintId: event.target.value }))
                          }
                          disabled={!selectedProjectId}
                        >
                          <option value="">No sprint</option>
                          {sprints.map((sprint) => (
                            <option key={sprint.id} value={sprint.id}>
                              {sprint.name}
                            </option>
                          ))}
                        </Select>
                      </Field>
                      <Field label="Assignee">
                        <Input
                          value={taskForm.assignee}
                          onChange={(event) =>
                            setTaskForm((current) => ({ ...current, assignee: event.target.value }))
                          }
                          placeholder="User identifier"
                          disabled={!selectedProjectId}
                        />
                      </Field>
                    </div>

                    <Field label="Labels" hint="Comma-separated">
                      <Input
                        value={taskForm.labels}
                        onChange={(event) =>
                          setTaskForm((current) => ({ ...current, labels: event.target.value }))
                        }
                        placeholder="backend, release, urgent"
                        disabled={!selectedProjectId}
                      />
                    </Field>

                    <Button type="submit" disabled={!selectedProjectId || busy.taskCreate}>
                      {busy.taskCreate ? 'Creating...' : 'Create task'}
                    </Button>
                  </div>
                </form>
              </SectionCard>
            </main>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
