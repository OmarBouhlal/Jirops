import { useMemo } from 'react';
import { Briefcase, Calendar, CheckSquare, Layers, Clock, Users, ArrowRight, Activity } from 'lucide-react';

function StatCard({ label, value, detail, icon: Icon, color = 'indigo' }) {
  const colorStyles = {
    indigo: 'from-indigo-500/10 to-purple-500/5 text-indigo-400 border-indigo-500/20',
    sky: 'from-sky-500/10 to-cyan-500/5 text-sky-400 border-sky-500/20',
    emerald: 'from-emerald-500/10 to-teal-500/5 text-emerald-400 border-emerald-500/20',
    pink: 'from-pink-500/10 to-rose-500/5 text-pink-400 border-pink-500/20',
  };

  return (
    <div className={`relative overflow-hidden rounded-3xl border bg-gradient-to-br p-6 shadow-md transition-all duration-300 hover:translate-y-[-2px] ${colorStyles[color]}`}>
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</span>
        <div className="rounded-2xl bg-white/5 p-2.5">
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <div className="mt-4 flex items-baseline gap-2">
        <span className="text-4xl font-bold tracking-tight text-white">{value}</span>
      </div>
      {detail && <p className="mt-2 text-xs text-slate-400">{detail}</p>}
    </div>
  );
}

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

export function DashboardView({ projects, sprints, tasks, activeProject, activeSprints, taskCounts, onViewChange }) {
  // Calculate sprint progress
  const activeSprintInfo = useMemo(() => {
    if (!activeSprints.length) return null;
    const active = activeSprints[0];
    
    // Filter tasks that belong to this sprint
    const sprintTasks = tasks.filter(t => t.sprintId === active.id);
    const total = sprintTasks.length;
    const completed = sprintTasks.filter(t => t.status === 'DONE').length;
    const percent = total > 0 ? Math.round((completed / total) * 100) : 0;
    
    return {
      ...active,
      totalTasks: total,
      completedTasks: completed,
      percentage: percent,
      tasks: sprintTasks,
    };
  }, [activeSprints, tasks]);

  return (
    <div className="space-y-6">
      {/* Top metrics bar */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 animate-fade-in">
        <StatCard 
          label="Total Projects" 
          value={projects.length} 
          detail="Active workspaces"
          icon={Briefcase}
          color="indigo"
        />
        <StatCard 
          label="Active Sprints" 
          value={activeSprints.length} 
          detail="Currently running"
          icon={Calendar}
          color="sky"
        />
        <StatCard 
          label="Active Project Tasks" 
          value={tasks.length} 
          detail={`${taskCounts.TODO || 0} todo, ${taskCounts.DONE || 0} done`}
          icon={CheckSquare}
          color="emerald"
        />
        <StatCard 
          label="Current Workspace" 
          value={activeProject ? activeProject.key : 'None'} 
          detail={activeProject ? activeProject.name : 'Select a project'}
          icon={Layers}
          color="pink"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Active Sprint Section */}
        <div className="lg:col-span-2 glass rounded-3xl p-6 shadow-glow flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-display text-xl font-bold text-white flex items-center gap-2">
                <Activity className="h-5 w-5 text-sky-400" />
                Active Sprint Progress
              </h3>
              {activeSprintInfo && (
                <span className="rounded-full border border-sky-500/20 bg-sky-500/10 px-3 py-1 text-xs font-semibold text-sky-300 uppercase">
                  Running
                </span>
              )}
            </div>

            {activeSprintInfo ? (
              <div className="space-y-6">
                <div>
                  <h4 className="text-2xl font-bold text-white">{activeSprintInfo.name}</h4>
                  <p className="mt-1 text-sm text-slate-300">{activeSprintInfo.goal || 'No goal specified for this sprint.'}</p>
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Completion</span>
                    <span className="font-semibold text-sky-300">{activeSprintInfo.percentage}% ({activeSprintInfo.completedTasks}/{activeSprintInfo.totalTasks} tasks)</span>
                  </div>
                  <div className="h-3 w-full overflow-hidden rounded-full bg-slate-950/50 p-[2px] border border-white/5">
                    <div 
                      className="h-full rounded-full bg-gradient-to-r from-sky-500 to-indigo-500 transition-all duration-500"
                      style={{ width: `${activeSprintInfo.percentage}%` }}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 rounded-2xl bg-white/5 p-4 border border-white/5">
                  <div className="flex items-center gap-3">
                    <Clock className="h-5 w-5 text-slate-400" />
                    <div>
                      <span className="block text-xs text-slate-400">Duration</span>
                      <span className="text-sm font-medium text-slate-200">
                        {formatDate(activeSprintInfo.startDate)} - {formatDate(activeSprintInfo.endDate)}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Users className="h-5 w-5 text-slate-400" />
                    <div>
                      <span className="block text-xs text-slate-400">Team Active</span>
                      <span className="text-sm font-medium text-slate-200">
                        {activeProject?.members?.length || 1} members
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <Calendar className="h-12 w-12 text-slate-500 stroke-[1.5]" />
                <h4 className="mt-4 font-semibold text-white">No active sprint</h4>
                <p className="mt-1 text-sm text-slate-400 max-w-xs">
                  Go to the Sprint Planner to launch an active sprint or create a new one.
                </p>
                <button
                  onClick={() => onViewChange('sprints')}
                  className="mt-4 inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-xs font-semibold text-slate-200 hover:bg-white/10 transition"
                >
                  Manage Sprints <ArrowRight className="h-3.5 w-3.5" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Workspace Project List */}
        <div className="glass rounded-3xl p-6 shadow-glow">
          <h3 className="font-display text-xl font-bold text-white mb-4 flex items-center gap-2">
            <Briefcase className="h-5 w-5 text-purple-400" />
            Projects Overview
          </h3>

          <div className="space-y-3">
            {projects.slice(0, 5).map((project) => {
              const isActive = activeProject && activeProject.id === project.id;
              return (
                <div 
                  key={project.id}
                  onClick={() => onViewChange('board', project.id)}
                  className={`group flex items-center justify-between gap-3 rounded-2xl border px-4 py-3 cursor-pointer transition ${
                    isActive 
                      ? 'border-indigo-500/30 bg-indigo-500/10 text-white' 
                      : 'border-white/5 bg-slate-950/20 hover:bg-white/5 hover:border-white/10'
                  }`}
                >
                  <div>
                    <h4 className="font-semibold text-white group-hover:text-indigo-400 transition">{project.name}</h4>
                    <p className="text-xs uppercase tracking-wider text-slate-400 mt-0.5">{project.key}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="flex items-center gap-1 text-xs text-slate-300 bg-white/5 px-2 py-1 rounded-lg">
                      <Users className="h-3 w-3" />
                      {project.members?.length || 0}
                    </span>
                    <ArrowRight className="h-4 w-4 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 text-slate-400 group-hover:text-indigo-400 transition-all" />
                  </div>
                </div>
              );
            })}
            
            {projects.length === 0 && (
              <p className="text-sm text-slate-400 text-center py-6">No projects yet.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
