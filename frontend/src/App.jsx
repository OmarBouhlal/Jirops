import { useEffect, useMemo, useState } from 'react';
import { api } from './lib/api';
import { loadSession, saveSession } from './lib/storage';

// Component imports
import { Sidebar } from './components/Sidebar';
import { DashboardView } from './components/DashboardView';
import { BoardView } from './components/BoardView';
import { SprintsView } from './components/SprintsView';
import { ProjectSettingsView } from './components/ProjectSettingsView';
import { ConfirmModal, EditProjectModal } from './components/Modal';
import { CreateTaskDrawer, CreateSprintDrawer, CreateProjectDrawer } from './components/Drawers';

import { Key, Mail, Lock, ShieldAlert, Award, Compass, Sparkles } from 'lucide-react';

const statusOrder = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
const statusLabels = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  DONE: 'Done',
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

function getTaskCounts(tasks) {
  return statusOrder.reduce((acc, status) => {
    acc[status] = tasks.filter((task) => task.status === status).length;
    return acc;
  }, {});
}

function suggestProjectKey(name) {
  return name
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 10);
}

function App() {
  const [session, setSession] = useState(() => loadSession() || null);
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState(emptyAuth);
  
  // Tab Navigation state
  const [activeTab, setActiveTab] = useState('dashboard');

  // Lists state
  const [projects, setProjects] = useState([]);
  const [sprints, setSprints] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');

  // Form states (controlled inputs)
  const [projectForm, setProjectForm] = useState(emptyProject);
  const [sprintForm, setSprintForm] = useState(emptySprint);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [projectMemberId, setProjectMemberId] = useState('');
  const [taskFilter, setTaskFilter] = useState('');

  // Modals & Drawers open states
  const [taskDrawerOpen, setTaskDrawerOpen] = useState(false);
  const [sprintDrawerOpen, setSprintDrawerOpen] = useState(false);
  const [projectDrawerOpen, setProjectDrawerOpen] = useState(false);
  
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    title: '',
    message: '',
    confirmText: 'Confirm',
    type: 'danger',
    onConfirm: () => {}
  });

  const [editProjectModal, setEditProjectModal] = useState({
    isOpen: false,
    project: null
  });

  // UI state
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

  // Sync session storage
  useEffect(() => {
    saveSession(session);
    if (session?.accessToken) {
      // Decode user details if any or standard load
    }
  }, [session]);

  // Set default project selection
  useEffect(() => {
    if (!projects.length) {
      setSelectedProjectId('');
      return;
    }

    if (!selectedProjectId || !projects.some((project) => project.id === selectedProjectId)) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  // Load sprints & tasks when workspace changes
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
  }, [selectedProjectId, session?.accessToken]);

  // Load projects list
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

  // Reset page view details when project changes
  useEffect(() => {
    setProjectMemberId('');
    setSprintForm(emptySprint);
    setTaskForm(emptyTask);
    
    // Automatically default to dashboard view if switching projects
    if (selectedProjectId) {
      // Keep current tab but make sure settings works
      if (activeTab === 'settings' && !selectedProjectId) {
        setActiveTab('dashboard');
      }
    }
  }, [selectedProjectId]);

  function notify(type, text) {
    setMessage({ type, text });
    // Auto clear notices after 4s
    setTimeout(() => {
      setMessage((current) => (current && current.text === text ? null : current));
    }, 4000);
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

      // Add email info dynamically to session
      setSession({
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
        email: authForm.email.trim()
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
      setActiveTab('dashboard');
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
      api.listTasks(token, { projectId }),
    ]);
    setSprints(Array.isArray(nextSprints) ? nextSprints : []);
    setTasks(Array.isArray(nextTasks) ? nextTasks : []);
  }

  // Projects Handlers
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
      setProjectDrawerOpen(false);
      notify('success', `Project ${response.key} created.`);
    } catch (error) {
      notify('error', error.message);
    } finally {
      setBusy((current) => ({ ...current, projectCreate: false }));
    }
  }

  function triggerProjectUpdate(project) {
    setEditProjectModal({
      isOpen: true,
      project: project
    });
  }

  async function handleProjectUpdateDetails(details) {
    if (!activeProject) return;
    try {
      const token = requireToken();
      await api.updateProject(token, activeProject.id, {
        name: details.name,
        description: details.description,
      });
      await refreshProjects(activeProject.id);
      await refreshWorkspace(activeProject.id);
      notify('success', 'Project updated.');
    } catch (error) {
      notify('error', error.message);
    }
  }

  function triggerProjectDelete(project) {
    setConfirmModal({
      isOpen: true,
      title: 'Delete Project',
      message: `Are you sure you want to delete "${project.name}" (${project.key})? This action cannot be undone and will delete all associated Sprints and Tasks.`,
      confirmText: 'Delete Project',
      type: 'danger',
      onConfirm: async () => {
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
    });
  }

  async function handleAddProjectMember(userId) {
    if (!selectedProjectId) return;
    try {
      const token = requireToken();
      await api.addProjectMember(token, selectedProjectId, { userId });
      await refreshProjects(selectedProjectId);
      notify('success', 'Member added.');
    } catch (error) {
      notify('error', error.message);
    }
  }

  // Sprints Handlers
  async function handleSprintSubmit(event) {
    event.preventDefault();
    if (!selectedProjectId) return;
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
      setSprintDrawerOpen(false);
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
      notify('success', `Sprint ${sprint.name} status updated.`);
    } catch (error) {
      notify('error', error.message);
    }
  }

  async function handleAddTaskToSprint(event, sprintId) {
    event.preventDefault();
    if (!selectedProjectId || !sprintId) return;
    const data = new FormData(event.currentTarget);
    const taskId = String(data.get('taskId') || '').trim();
    if (!taskId) return;

    try {
      const token = requireToken();
      await api.addTaskToSprint(token, sprintId, { taskId });
      await refreshWorkspace(selectedProjectId);
      notify('success', 'Task assigned to sprint.');
      event.currentTarget.reset();
    } catch (error) {
      notify('error', error.message);
    }
  }

  // Tasks Handlers
  async function handleTaskSubmit(event) {
    event.preventDefault();
    if (!selectedProjectId) return;
    setBusy((current) => ({ ...current, taskCreate: true }));
    setMessage(null);

    try {
      const token = requireToken();
      
      const splitLabels = (raw) => raw.split(',').map((label) => label.trim()).filter(Boolean);

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
      setTaskDrawerOpen(false);
      await refreshWorkspace(selectedProjectId);
      notify('success', 'Task created successfully.');
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

  function triggerTaskDelete(task) {
    setConfirmModal({
      isOpen: true,
      title: 'Delete Task',
      message: `Are you sure you want to delete the task "${task.title}"? This action cannot be undone.`,
      confirmText: 'Delete Task',
      type: 'danger',
      onConfirm: async () => {
        try {
          const token = requireToken();
          await api.deleteTask(token, task.id);
          await refreshWorkspace(selectedProjectId);
          notify('success', 'Task deleted.');
        } catch (error) {
          notify('error', error.message);
        }
      }
    });
  }

  // Helper to change view from dashboard
  const handleDashboardViewChange = (viewId, projectId = null) => {
    if (projectId) {
      setSelectedProjectId(projectId);
    }
    setActiveTab(viewId);
  };

  const isAuthenticated = Boolean(session?.accessToken);

  return (
    <div className="noise relative min-h-screen overflow-hidden">
      {/* Background gradients */}
      <div className="absolute -left-32 top-24 h-96 w-96 rounded-full bg-indigo-500/10 blur-3xl pointer-events-none" />
      <div className="absolute right-0 top-20 h-[30rem] w-[30rem] rounded-full bg-cyan-400/8 blur-3xl pointer-events-none" />

      {/* Toast Notification */}
      {message && (
        <div
          className={`fixed right-6 top-6 z-50 rounded-2xl border px-5 py-4 text-xs font-semibold shadow-xl backdrop-blur-md animate-scale-up ${
            message.type === 'error'
              ? 'border-rose-500/20 bg-rose-950/80 text-rose-200'
              : message.type === 'success'
                ? 'border-emerald-500/20 bg-emerald-950/80 text-emerald-200'
                : 'border-slate-500/20 bg-slate-900/80 text-slate-200'
          }`}
        >
          {message.text}
        </div>
      )}

      {/* Modals & Drawers */}
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        onClose={() => setConfirmModal(prev => ({ ...prev, isOpen: false }))}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
        confirmText={confirmModal.confirmText}
        type={confirmModal.type}
      />

      <EditProjectModal
        isOpen={editProjectModal.isOpen}
        onClose={() => setEditProjectModal(prev => ({ ...prev, isOpen: false }))}
        onSave={handleProjectUpdateDetails}
        project={editProjectModal.project}
      />

      <CreateTaskDrawer
        isOpen={taskDrawerOpen}
        onClose={() => setTaskDrawerOpen(false)}
        onSubmit={handleTaskSubmit}
        sprints={sprints}
        taskForm={taskForm}
        setTaskForm={setTaskForm}
        busy={busy.taskCreate}
      />

      <CreateSprintDrawer
        isOpen={sprintDrawerOpen}
        onClose={() => setSprintDrawerOpen(false)}
        onSubmit={handleSprintSubmit}
        sprintForm={sprintForm}
        setSprintForm={setSprintForm}
        busy={busy.sprintCreate}
      />

      <CreateProjectDrawer
        isOpen={projectDrawerOpen}
        onClose={() => setProjectDrawerOpen(false)}
        onSubmit={handleProjectSubmit}
        projectForm={projectForm}
        setProjectForm={setProjectForm}
        busy={busy.projectCreate}
      />

      {/* Main UI Layout */}
      {!isAuthenticated ? (
        // Login Page Layout
        <div className="relative flex min-h-screen items-center justify-center p-4">
          <div className="grid w-full max-w-5xl gap-8 lg:grid-cols-[1.2fr_0.8fr] items-center">
            {/* Visual Intro Column */}
            <section className="relative overflow-hidden rounded-[2.5rem] border border-white/5 bg-slate-900/30 p-10 shadow-2xl backdrop-blur-md">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(99,102,241,0.12),transparent_40%)]" />
              
              <div className="relative">
                <span className="inline-flex items-center gap-1 rounded-full border border-indigo-400/20 bg-indigo-400/10 px-3 py-1 text-xs font-semibold text-indigo-300">
                  <Sparkles className="h-3 w-3" /> Calm Control Room
                </span>
                
                <h2 className="mt-6 font-display text-5xl font-extrabold leading-tight text-white tracking-tight">
                  Calm & structured control for <span className="bg-gradient-to-r from-indigo-400 to-cyan-300 bg-clip-text text-transparent">product builders.</span>
                </h2>
                
                <p className="mt-6 text-sm leading-relaxed text-slate-300 max-w-xl">
                  Connect developers, orchestrate milestones, and monitor workspace actions from one single dashboard. Designed with extreme legibility and responsiveness in mind.
                </p>

                <div className="mt-10 grid gap-4 sm:grid-cols-3">
                  <div className="rounded-2xl border border-white/5 bg-slate-950/30 p-4">
                    <Award className="h-5 w-5 text-indigo-400" />
                    <span className="block text-xl font-bold text-white mt-2">3 Layers</span>
                    <span className="text-[10px] text-slate-400">Projects, sprints & tasks</span>
                  </div>
                  <div className="rounded-2xl border border-white/5 bg-slate-950/30 p-4">
                    <Compass className="h-5 w-5 text-cyan-400" />
                    <span className="block text-xl font-bold text-white mt-2">Smooth UI</span>
                    <span className="text-[10px] text-slate-400">Sidebar navigation</span>
                  </div>
                  <div className="rounded-2xl border border-white/5 bg-slate-950/30 p-4">
                    <Key className="h-5 w-5 text-emerald-400" />
                    <span className="block text-xl font-bold text-white mt-2">Secure</span>
                    <span className="text-[10px] text-slate-400">OAuth gateway token flow</span>
                  </div>
                </div>
              </div>
            </section>

            {/* Auth Form Column */}
            <section className="glass rounded-[2.5rem] p-8 shadow-2xl">
              <div className="mb-6 flex rounded-2xl border border-white/10 bg-slate-950/40 p-1">
                <button
                  className={`flex-1 rounded-xl px-4 py-2.5 text-xs font-semibold transition ${
                    authMode === 'login' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
                  }`}
                  onClick={() => setAuthMode('login')}
                  type="button"
                >
                  Sign In
                </button>
                <button
                  className={`flex-1 rounded-xl px-4 py-2.5 text-xs font-semibold transition ${
                    authMode === 'register' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
                  }`}
                  onClick={() => setAuthMode('register')}
                  type="button"
                >
                  Register
                </button>
              </div>

              <form className="space-y-4" onSubmit={handleAuthSubmit}>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-slate-300">Email Address</span>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                    <input
                      type="email"
                      required
                      autoComplete="email"
                      value={authForm.email}
                      onChange={(e) => setAuthForm(prev => ({ ...prev, email: e.target.value }))}
                      className="w-full rounded-2xl border border-white/10 bg-slate-950/40 py-3 pl-10 pr-4 text-sm text-white outline-none focus:border-indigo-500/50 transition placeholder:text-slate-600"
                      placeholder="you@company.com"
                    />
                  </div>
                </label>

                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-slate-300">Password</span>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                    <input
                      type="password"
                      required
                      autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                      value={authForm.password}
                      onChange={(e) => setAuthForm(prev => ({ ...prev, password: e.target.value }))}
                      className="w-full rounded-2xl border border-white/10 bg-slate-950/40 py-3 pl-10 pr-4 text-sm text-white outline-none focus:border-indigo-500/50 transition placeholder:text-slate-600"
                      placeholder="••••••••"
                    />
                  </div>
                  {authMode === 'register' && (
                    <span className="mt-1 block text-[10px] text-slate-400">Must contain at least 8 characters.</span>
                  )}
                </label>

                <button
                  type="submit"
                  disabled={busy.auth}
                  className="w-full rounded-2xl bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-bold py-3.5 shadow-md shadow-indigo-500/10 hover:brightness-110 active:scale-[0.98] transition disabled:opacity-50 text-sm"
                >
                  {busy.auth ? 'Please wait...' : authMode === 'login' ? 'Enter Control Room' : 'Create Workspace Account'}
                </button>
              </form>
            </section>
          </div>
        </div>
      ) : (
        // Dashboard Authed Layout
        <div className="relative mx-auto flex min-h-screen max-w-[1600px] gap-6 px-4 py-4 sm:px-6 lg:px-8">
          {/* Left Sidebar */}
          <Sidebar
            projects={projects}
            selectedProjectId={selectedProjectId}
            setSelectedProjectId={setSelectedProjectId}
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            session={session}
            handleLogout={handleLogout}
            onCreateProjectClick={() => setProjectDrawerOpen(true)}
          />

          {/* Right Main Panel */}
          <div className="flex-1 min-w-0 space-y-4">
            {/* Top workspace stats header */}
            <header className="glass rounded-[2rem] px-6 py-4 shadow-glow flex flex-wrap items-center justify-between gap-4">
              <div>
                <span className="text-[9px] uppercase tracking-[0.25em] text-indigo-400 font-bold">WORKSPACE ACTION HUB</span>
                <div className="flex items-center gap-3 mt-0.5">
                  <h1 className="font-display text-2xl font-bold text-white">
                    {activeProject ? activeProject.name : 'Workspace Control'}
                  </h1>
                  <span className="rounded-full border border-indigo-500/20 bg-indigo-500/10 px-2.5 py-0.5 text-[10px] font-bold text-indigo-300 uppercase">
                    {activeProject ? activeProject.key : 'ALL'}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={() => setTaskDrawerOpen(true)}
                  disabled={!selectedProjectId}
                  className="rounded-2xl border border-white/10 bg-white/5 px-4.5 py-2.5 text-xs font-semibold text-slate-200 hover:bg-white/10 transition disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  + Create Task
                </button>
                <button
                  onClick={() => setSprintDrawerOpen(true)}
                  disabled={!selectedProjectId}
                  className="rounded-2xl bg-indigo-600 hover:bg-indigo-500 px-4.5 py-2.5 text-xs font-bold text-white shadow shadow-indigo-600/10 transition disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  + Plan Sprint
                </button>
              </div>
            </header>

            {/* Sub-view switcher based on Tab */}
            <main className="min-h-[500px]">
              {activeTab === 'dashboard' && (
                <DashboardView
                  projects={projects}
                  sprints={sprints}
                  tasks={tasks}
                  activeProject={activeProject}
                  activeSprints={activeSprints}
                  taskCounts={taskCounts}
                  onViewChange={handleDashboardViewChange}
                />
              )}

              {activeTab === 'board' && (
                <BoardView
                  tasks={tasks}
                  activeProject={activeProject}
                  onStatusChange={handleTaskStatusChange}
                  onDelete={triggerTaskDelete}
                  onCreateTaskClick={() => setTaskDrawerOpen(true)}
                />
              )}

              {activeTab === 'sprints' && (
                <SprintsView
                  sprints={sprints}
                  tasks={tasks}
                  activeProject={activeProject}
                  onCreateSprintClick={() => setSprintDrawerOpen(true)}
                  handleSprintAction={handleSprintAction}
                  handleAddTaskToSprint={handleAddTaskToSprint}
                />
              )}

              {activeTab === 'settings' && (
                <ProjectSettingsView
                  activeProject={activeProject}
                  onUpdateClick={() => triggerProjectUpdate(activeProject)}
                  onDeleteClick={() => triggerProjectDelete(activeProject)}
                  onAddMember={handleAddProjectMember}
                  projectMemberId={projectMemberId}
                  setProjectMemberId={setProjectMemberId}
                />
              )}
            </main>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
