import { useState, useMemo } from 'react';
import { 
  Plus, 
  Search, 
  Trash2, 
  ChevronRight, 
  Paperclip, 
  MessageSquare,
  AlertTriangle,
  AlertCircle,
  Clock,
  ArrowRight
} from 'lucide-react';
import { getAvatarDetails } from './Sidebar';

const statusOrder = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
const statusLabels = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
};

const statusTone = {
  TODO: 'border-slate-500/30 bg-slate-500/10 text-slate-300',
  IN_PROGRESS: 'border-sky-500/30 bg-sky-500/10 text-sky-400',
  IN_REVIEW: 'border-amber-500/30 bg-amber-500/10 text-amber-400',
  DONE: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400',
};

const priorityConfig = {
  LOW: { color: 'text-slate-400 bg-slate-500/10 border-slate-500/20', label: 'Low' },
  MEDIUM: { color: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20', label: 'Medium' },
  HIGH: { color: 'text-amber-400 bg-amber-500/10 border-amber-500/20', label: 'High' },
  CRITICAL: { color: 'text-rose-400 bg-rose-500/10 border-rose-500/20', label: 'Critical' },
};

function TaskCard({ task, onStatusChange, onDelete }) {
  const avatar = getAvatarDetails(task.assignee || 'Unassigned');
  const reporterAvatar = getAvatarDetails(task.reporter || 'Unknown');
  const priority = priorityConfig[task.priority] || priorityConfig.MEDIUM;

  return (
    <article className="group relative rounded-2xl border border-white/5 bg-slate-900/60 p-4 shadow-lg hover:border-indigo-500/20 hover:bg-slate-900/80 transition-all duration-200">
      <div className="flex items-start justify-between gap-3">
        <h4 className="font-semibold text-white leading-snug group-hover:text-indigo-300 transition-colors">
          {task.title}
        </h4>
        <span className={`rounded-lg border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${priority.color}`}>
          {priority.label}
        </span>
      </div>

      {task.description && (
        <p className="mt-2 text-xs text-slate-400 leading-relaxed line-clamp-3">
          {task.description}
        </p>
      )}

      {/* Labels */}
      {task.labels && task.labels.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1">
          {task.labels.map((label) => (
            <span key={label} className="rounded-md bg-white/5 border border-white/5 px-2 py-0.5 text-[9px] font-semibold text-slate-300">
              {label}
            </span>
          ))}
        </div>
      )}

      {/* Meta Row */}
      <div className="mt-4 flex items-center justify-between border-t border-white/5 pt-3 text-[10px] text-slate-400">
        <div className="flex items-center gap-3">
          {/* Assignee Avatar */}
          <div className="flex items-center gap-1.5" title={`Assignee: ${task.assignee || 'Unassigned'}`}>
            <div className={`flex h-5 w-5 items-center justify-center rounded-full bg-gradient-to-br ${avatar.gradient} text-[8px] font-bold text-white`}>
              {avatar.initials}
            </div>
            <span className="max-w-[60px] truncate text-[9px]">
              {task.assignee ? (task.assignee.includes('-') ? 'Assigned' : task.assignee) : 'Unassigned'}
            </span>
          </div>
        </div>

        {/* Counts (Comments / Attachments) */}
        <div className="flex items-center gap-2">
          {task.comments && task.comments.length > 0 && (
            <span className="flex items-center gap-1">
              <MessageSquare className="h-3 w-3 stroke-[1.5]" />
              {task.comments.length}
            </span>
          )}
          {task.attachments && task.attachments.length > 0 && (
            <span className="flex items-center gap-1">
              <Paperclip className="h-3 w-3 stroke-[1.5]" />
              {task.attachments.length}
            </span>
          )}
        </div>
      </div>

      {/* Action Row */}
      <div className="mt-4 flex items-center gap-2 border-t border-white/5 pt-3">
        <div className="flex-1">
          <select
            value={task.status}
            onChange={(e) => onStatusChange(task, e.target.value)}
            className="w-full rounded-xl border border-white/10 bg-slate-950/40 px-2.5 py-1.5 text-[11px] text-slate-200 outline-none focus:border-indigo-500/50 transition cursor-pointer"
          >
            {statusOrder.map((status) => (
              <option key={status} value={status}>
                Move: {statusLabels[status]}
              </option>
            ))}
          </select>
        </div>
        <button
          onClick={() => onDelete(task)}
          className="rounded-xl border border-white/10 bg-white/5 p-1.5 text-slate-400 hover:bg-rose-500/10 hover:text-rose-400 transition"
          title="Delete task"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </article>
  );
}

export function BoardView({ tasks, onStatusChange, onDelete, onCreateTaskClick, activeProject }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');

  // Filter tasks locally based on query and priority
  const filteredTasks = useMemo(() => {
    return tasks.filter((task) => {
      const matchesSearch = 
        task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (task.description && task.description.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesPriority = priorityFilter ? task.priority === priorityFilter : true;
      return matchesSearch && matchesPriority;
    });
  }, [tasks, searchQuery, priorityFilter]);

  return (
    <div className="space-y-6">
      {/* Board controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/10 pb-4 animate-fade-in">
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search tasks..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-64 rounded-2xl border border-white/10 bg-slate-950/40 py-2.5 pl-10 pr-4 text-xs text-white outline-none focus:border-indigo-500/50 transition placeholder:text-slate-500"
            />
          </div>
          
          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="rounded-2xl border border-white/10 bg-slate-950/40 px-3 py-2.5 text-xs text-slate-300 outline-none focus:border-indigo-500/50 transition cursor-pointer"
          >
            <option value="">All Priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        <button
          onClick={onCreateTaskClick}
          disabled={!activeProject}
          className="inline-flex items-center gap-1.5 rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-600/10 transition"
        >
          <Plus className="h-4 w-4" />
          Create Task
        </button>
      </div>

      {/* Kanban Board Columns */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 items-start">
        {statusOrder.map((status) => {
          const columnTasks = filteredTasks.filter((task) => task.status === status);

          return (
            <div key={status} className="flex flex-col max-h-[calc(100vh-12rem)] rounded-3xl border border-white/5 bg-slate-950/20 p-4 shadow-sm">
              {/* Column Header */}
              <div className="flex items-center justify-between mb-4 pb-2 border-b border-white/5">
                <div>
                  <h3 className="font-display font-bold text-white text-sm">
                    {statusLabels[status]}
                  </h3>
                  <span className="text-[10px] uppercase tracking-wider text-slate-500 font-semibold mt-0.5 block">
                    {columnTasks.length} {columnTasks.length === 1 ? 'item' : 'items'}
                  </span>
                </div>
                <span className={`rounded-full border px-2 py-0.5 text-[9px] font-semibold uppercase tracking-wider ${statusTone[status]}`}>
                  {status}
                </span>
              </div>

              {/* Task list container */}
              <div className="space-y-3 overflow-y-auto pr-1 flex-1 min-h-[200px]">
                {columnTasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    onStatusChange={onStatusChange}
                    onDelete={onDelete}
                  />
                ))}
                
                {columnTasks.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-white/5 bg-slate-900/10 p-5 text-center text-xs text-slate-500">
                    No tasks here
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
