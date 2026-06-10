import { useState } from 'react';
import { 
  Plus, 
  Calendar, 
  Play, 
  CheckCircle, 
  ChevronDown, 
  ChevronRight, 
  Layers, 
  FolderPlus,
  User,
  Clock
} from 'lucide-react';
import { getAvatarDetails } from './Sidebar';

const statusTone = {
  PLANNED: 'border-slate-500/30 bg-slate-500/10 text-slate-300',
  ACTIVE: 'border-sky-500/30 bg-sky-500/10 text-sky-400',
  CLOSED: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400',
};

function formatDate(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (isNaN(date.getTime())) return 'Not set';
  return new Intl.DateTimeFormat('en', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date);
}

export function SprintsView({ 
  sprints, 
  tasks, 
  onCreateSprintClick, 
  handleSprintAction, 
  handleAddTaskToSprint, 
  activeProject 
}) {
  const [expandedSprints, setExpandedSprints] = useState({});

  const toggleSprintExpand = (id) => {
    setExpandedSprints(prev => ({ ...prev, [id]: !prev[id] }));
  };

  // Sprints categorized
  const activeSprints = sprints.filter(s => s.status === 'ACTIVE');
  const plannedSprints = sprints.filter(s => s.status === 'PLANNED');
  const closedSprints = sprints.filter(s => s.status === 'CLOSED');

  // Backlog Tasks (not in any sprint)
  const backlogTasks = tasks.filter(t => !t.sprintId);

  const renderSprintCard = (sprint) => {
    const isExpanded = expandedSprints[sprint.id];
    // Find tasks in this sprint
    const sprintTasks = tasks.filter(t => t.sprintId === sprint.id);

    return (
      <div 
        key={sprint.id}
        className="rounded-3xl border border-white/5 bg-slate-900/40 overflow-hidden transition-all duration-200"
      >
        {/* Sprint header */}
        <div className="flex flex-wrap items-center justify-between gap-4 p-5 bg-white/[0.01] border-b border-white/5">
          <div className="flex items-center gap-3">
            <button 
              onClick={() => toggleSprintExpand(sprint.id)}
              className="rounded-xl p-1 text-slate-400 hover:bg-white/5 hover:text-white transition"
            >
              {isExpanded ? <ChevronDown className="h-5 w-5" /> : <ChevronRight className="h-5 w-5" />}
            </button>
            <div>
              <h4 className="font-display font-bold text-white text-base">{sprint.name}</h4>
              <p className="text-xs text-slate-400 mt-0.5">{sprint.goal || 'No goal set'}</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className={`rounded-full border px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider ${statusTone[sprint.status]}`}>
              {sprint.status}
            </span>

            {/* Quick action buttons */}
            <div className="flex gap-2">
              {sprint.status === 'PLANNED' && (
                <button
                  type="button"
                  onClick={() => handleSprintAction(async (token, id) => {
                    const response = await fetch(`/sprints/${id}/start`, {
                      method: 'POST',
                      headers: { Authorization: `Bearer ${token}` }
                    });
                    if (!response.ok) throw new Error('Failed to start sprint');
                  }, sprint)}
                  className="inline-flex items-center gap-1 rounded-xl bg-sky-600 hover:bg-sky-500 px-3 py-1.5 text-xs font-semibold text-white transition"
                >
                  <Play className="h-3 w-3" /> Start
                </button>
              )}
              {sprint.status === 'ACTIVE' && (
                <button
                  type="button"
                  onClick={() => handleSprintAction(async (token, id) => {
                    const response = await fetch(`/sprints/${id}/complete`, {
                      method: 'POST',
                      headers: { Authorization: `Bearer ${token}` }
                    });
                    if (!response.ok) throw new Error('Failed to complete sprint');
                  }, sprint)}
                  className="inline-flex items-center gap-1 rounded-xl bg-emerald-600 hover:bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition"
                >
                  <CheckCircle className="h-3 w-3" /> Complete
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Sprint Info Row */}
        <div className="grid grid-cols-2 gap-4 px-6 py-3 border-b border-white/5 bg-slate-950/20 text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <Clock className="h-3.5 w-3.5 text-slate-500" />
            <span>Dates: {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}</span>
          </div>
          <div className="flex items-center gap-2">
            <Layers className="h-3.5 w-3.5 text-slate-500" />
            <span>Tasks: {sprintTasks.length} total</span>
          </div>
        </div>

        {/* Tasks Expandable Section */}
        {isExpanded && (
          <div className="p-4 space-y-3 bg-slate-950/10">
            {sprintTasks.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-white/5 text-slate-500">
                      <th className="pb-2 font-semibold">Title</th>
                      <th className="pb-2 font-semibold">Status</th>
                      <th className="pb-2 font-semibold">Priority</th>
                      <th className="pb-2 font-semibold">Assignee</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sprintTasks.map(t => {
                      const avatar = getAvatarDetails(t.assignee || 'Unassigned');
                      return (
                        <tr key={t.id} className="border-b border-white/5 last:border-0 text-slate-300">
                          <td className="py-2.5 font-medium text-white">{t.title}</td>
                          <td className="py-2.5">
                            <span className="rounded-md bg-white/5 px-2 py-0.5 text-[9px] uppercase font-semibold">
                              {t.status}
                            </span>
                          </td>
                          <td className="py-2.5 font-semibold uppercase">{t.priority}</td>
                          <td className="py-2.5">
                            <div className="flex items-center gap-1.5">
                              <div className={`h-4.5 w-4.5 flex items-center justify-center rounded-full bg-gradient-to-br ${avatar.gradient} text-[7px] font-bold text-white`}>
                                {avatar.initials}
                              </div>
                              <span className="truncate max-w-[80px]">
                                {t.assignee ? (t.assignee.includes('-') ? 'Assigned' : t.assignee) : 'Unassigned'}
                              </span>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-xs text-slate-500 text-center py-4">No tasks currently assigned to this sprint.</p>
            )}

            {/* Form to add task to sprint */}
            {sprint.status !== 'CLOSED' && (
              <form 
                onSubmit={(e) => handleAddTaskToSprint(e, sprint.id)}
                className="flex items-center gap-2 border-t border-white/5 pt-3"
              >
                <select 
                  name="taskId" 
                  defaultValue=""
                  required
                  className="flex-1 rounded-xl border border-white/10 bg-slate-950/40 px-3 py-2 text-xs text-slate-200 outline-none focus:border-indigo-500/50 transition cursor-pointer"
                >
                  <option value="">Choose task from backlog...</option>
                  {backlogTasks.map(task => (
                    <option key={task.id} value={task.id}>
                      {task.title} ({task.priority})
                    </option>
                  ))}
                </select>
                <button 
                  type="submit"
                  className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 px-4 py-2 text-xs font-semibold text-white transition"
                >
                  <FolderPlus className="h-3.5 w-3.5" /> Assign
                </button>
              </form>
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/10 pb-4 animate-fade-in">
        <div>
          <h3 className="font-display text-xl font-bold text-white">Sprint Planner</h3>
          <p className="text-xs text-slate-400 mt-0.5">Manage delivery iterations and backlog prioritization.</p>
        </div>
        
        <button
          onClick={onCreateSprintClick}
          disabled={!activeProject}
          className="inline-flex items-center gap-1.5 rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-600/10 transition"
        >
          <Plus className="h-4 w-4" />
          Create Sprint
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Sprints Column */}
        <div className="lg:col-span-2 space-y-4">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Sprints</h3>
          
          <div className="space-y-3">
            {activeSprints.map(renderSprintCard)}
            {plannedSprints.map(renderSprintCard)}
            {closedSprints.map(renderSprintCard)}

            {sprints.length === 0 && (
              <div className="rounded-3xl border border-dashed border-white/10 bg-white/5 p-8 text-center text-sm text-slate-400">
                <Calendar className="h-10 w-10 text-slate-500 mx-auto stroke-[1.5]" />
                <h4 className="mt-4 font-semibold text-white">No sprints planned</h4>
                <p className="mt-1 text-xs text-slate-500">Plan delivery rhythms for this project by creating a sprint.</p>
              </div>
            )}
          </div>
        </div>

        {/* Backlog Column */}
        <div className="glass rounded-3xl p-6 shadow-glow flex flex-col max-h-[calc(100vh-12rem)]">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-4 flex items-center justify-between">
            <span>Product Backlog</span>
            <span className="rounded-full bg-white/5 px-2 py-0.5 text-[10px] text-slate-400">{backlogTasks.length} items</span>
          </h3>

          <div className="space-y-3 overflow-y-auto pr-1 flex-1">
            {backlogTasks.map((task) => {
              const priority = t => {
                if (t === 'LOW') return 'text-slate-400 bg-slate-500/10';
                if (t === 'MEDIUM') return 'text-cyan-400 bg-cyan-500/10';
                if (t === 'HIGH') return 'text-amber-400 bg-amber-500/10';
                return 'text-rose-400 bg-rose-500/10';
              };

              return (
                <div 
                  key={task.id}
                  className="rounded-2xl border border-white/5 bg-slate-900/50 p-3 hover:border-white/10 transition-colors"
                >
                  <h4 className="font-semibold text-white text-xs leading-snug">{task.title}</h4>
                  <div className="mt-2.5 flex items-center justify-between">
                    <span className={`rounded-md px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wider ${priority(task.priority)}`}>
                      {task.priority}
                    </span>
                    <span className="text-[9px] text-slate-400 uppercase font-semibold">
                      {task.status}
                    </span>
                  </div>
                </div>
              );
            })}

            {backlogTasks.length === 0 && (
              <div className="rounded-2xl border border-dashed border-white/5 bg-slate-900/10 p-5 text-center text-xs text-slate-500">
                Backlog is empty
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
