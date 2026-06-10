import { useEffect } from 'react';
import { X } from 'lucide-react';
import { formatUserLabel } from './Sidebar';

function BaseDrawer({ isOpen, onClose, title, children }) {
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-40 flex justify-end animate-fade-in">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Slide-out Panel */}
      <div className="relative z-10 flex h-full w-full max-w-lg flex-col border-l border-white/10 bg-slate-900/95 p-6 shadow-2xl backdrop-blur-md animate-slide-in-right">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-4 mb-6">
          <h2 className="font-display text-2xl font-bold text-white">{title}</h2>
          <button 
            type="button"
            onClick={onClose} 
            className="rounded-xl p-1.5 text-slate-400 hover:bg-white/5 hover:text-white transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto pr-2">
          {children}
        </div>
      </div>
    </div>
  );
}

export function CreateTaskDrawer({
  isOpen,
  onClose,
  onSubmit,
  userDirectory,
  projects,
  sprints,
  taskForm,
  setTaskForm,
  busy,
  activeProject,
}) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(e);
  };

  const selectedProject =
    (projects || []).find((project) => project.id === taskForm.projectId) || activeProject || null;

  const assigneeOptions = Array.from(
    new Set([
      selectedProject?.ownerId,
      ...(selectedProject?.members || []),
    ].filter(Boolean)),
  ).map((memberId) => ({
    id: memberId,
    label: formatUserLabel(memberId, userDirectory),
  }));

  return (
    <BaseDrawer isOpen={isOpen} onClose={onClose} title="Create Task">
      <form onSubmit={handleSubmit} className="space-y-5">
        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Project</span>
          <select
            value={taskForm.projectId}
            onChange={(e) =>
              setTaskForm((prev) => ({
                ...prev,
                projectId: e.target.value,
                sprintId: '',
                assignee: '',
              }))
            }
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
          >
            <option value="">Select project</option>
            {(projects || []).map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Title</span>
          <input
            type="text"
            required
            value={taskForm.title}
            onChange={(e) => setTaskForm(prev => ({ ...prev, title: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., Finalize release notes"
          />
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">Priority</span>
            <select
              value={taskForm.priority}
              onChange={(e) => setTaskForm(prev => ({ ...prev, priority: e.target.value }))}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">Sprint</span>
            <select
              value={taskForm.sprintId}
              onChange={(e) => setTaskForm(prev => ({ ...prev, sprintId: e.target.value }))}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            >
              <option value="">No sprint (Backlog)</option>
              {sprints.filter((sprint) => !taskForm.projectId || sprint.projectId === taskForm.projectId).map((sprint) => (
                <option key={sprint.id} value={sprint.id}>
                  {sprint.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Assignee</span>
          <select
            value={taskForm.assignee}
            onChange={(e) => setTaskForm((prev) => ({ ...prev, assignee: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition disabled:cursor-not-allowed disabled:opacity-60"
            disabled={!assigneeOptions.length}
          >
            <option value="">
              {assigneeOptions.length ? 'Unassigned' : 'No project members available'}
            </option>
            {assigneeOptions.map((member) => (
              <option key={member.id} value={member.id}>
                {member.label}
              </option>
            ))}
          </select>
          <span className="mt-1 block text-xs text-slate-400">
            Pick a teammate from the selected project instead of typing an identifier manually.
          </span>
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Labels</span>
          <input
            type="text"
            value={taskForm.labels}
            onChange={(e) => setTaskForm(prev => ({ ...prev, labels: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., backend, release, urgent (comma-separated)"
          />
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Description</span>
          <textarea
            rows={5}
            value={taskForm.description}
            onChange={(e) => setTaskForm(prev => ({ ...prev, description: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition resize-none"
            placeholder="Detail what needs to be done..."
          />
        </label>

        <div className="border-t border-white/10 pt-4 flex gap-3 justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-white/10 bg-white/5 px-5 py-3 text-sm font-semibold text-slate-300 hover:bg-white/10 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={busy}
            className="rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-6 py-3 text-sm font-semibold text-white transition focus:ring-2 focus:ring-indigo-500/20"
          >
            {busy ? 'Creating...' : 'Create Task'}
          </button>
        </div>
      </form>
    </BaseDrawer>
  );
}

export function CreateSprintDrawer({ isOpen, onClose, onSubmit, sprintForm, setSprintForm, busy }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(e);
  };

  return (
    <BaseDrawer isOpen={isOpen} onClose={onClose} title="Create Sprint">
      <form onSubmit={handleSubmit} className="space-y-5">
        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Sprint Name</span>
          <input
            type="text"
            required
            value={sprintForm.name}
            onChange={(e) => setSprintForm(prev => ({ ...prev, name: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., Sprint 12"
          />
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Goal</span>
          <input
            type="text"
            value={sprintForm.goal}
            onChange={(e) => setSprintForm(prev => ({ ...prev, goal: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., Ship onboarding flow"
          />
        </label>

        <div className="grid grid-cols-2 gap-4">
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">Start Date</span>
            <input
              type="date"
              value={sprintForm.startDate}
              onChange={(e) => setSprintForm(prev => ({ ...prev, startDate: e.target.value }))}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            />
          </label>

          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">End Date</span>
            <input
              type="date"
              value={sprintForm.endDate}
              onChange={(e) => setSprintForm(prev => ({ ...prev, endDate: e.target.value }))}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            />
          </label>
        </div>

        <div className="border-t border-white/10 pt-4 flex gap-3 justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-white/10 bg-white/5 px-5 py-3 text-sm font-semibold text-slate-300 hover:bg-white/10 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={busy}
            className="rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-6 py-3 text-sm font-semibold text-white transition focus:ring-2 focus:ring-indigo-500/20"
          >
            {busy ? 'Planning...' : 'Create Sprint'}
          </button>
        </div>
      </form>
    </BaseDrawer>
  );
}

export function CreateProjectDrawer({ isOpen, onClose, onSubmit, projectForm, setProjectForm, busy }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(e);
  };

  return (
    <BaseDrawer isOpen={isOpen} onClose={onClose} title="Create New Project">
      <form onSubmit={handleSubmit} className="space-y-5">
        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Project Name</span>
          <input
            type="text"
            required
            value={projectForm.name}
            onChange={(e) => {
              const name = e.target.value;
              const autoKey = name.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10);
              setProjectForm(prev => ({
                ...prev,
                name,
                key: prev.key ? prev.key : autoKey
              }));
            }}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., Launch Pad"
          />
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Project Key</span>
          <input
            type="text"
            required
            value={projectForm.key}
            onChange={(e) => setProjectForm(prev => ({ ...prev, key: e.target.value.toUpperCase() }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
            placeholder="e.g., LP"
          />
          <span className="mt-1 block text-xs text-slate-400">Uppercase letters and numbers only. Max 10 chars.</span>
        </label>

        <label className="block">
          <span className="mb-1.5 block text-sm font-medium text-slate-200">Description</span>
          <textarea
            rows={5}
            value={projectForm.description}
            onChange={(e) => setProjectForm(prev => ({ ...prev, description: e.target.value }))}
            className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition resize-none"
            placeholder="Brief description of the project workspace..."
          />
        </label>

        <div className="border-t border-white/10 pt-4 flex gap-3 justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-white/10 bg-white/5 px-5 py-3 text-sm font-semibold text-slate-300 hover:bg-white/10 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={busy}
            className="rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-6 py-3 text-sm font-semibold text-white transition focus:ring-2 focus:ring-indigo-500/20"
          >
            {busy ? 'Creating...' : 'Create Project'}
          </button>
        </div>
      </form>
    </BaseDrawer>
  );
}
