import { useState, useRef, useEffect } from 'react';
import { 
  LayoutDashboard, 
  KanbanSquare, 
  CalendarRange, 
  Settings, 
  LogOut, 
  ChevronDown, 
  Plus, 
  Briefcase,
  Users
} from 'lucide-react';

// Simple helper to generate initials and gradient colors for avatar based on email/string
export function getAvatarDetails(text = '') {
  if (!text) return { initials: 'U', gradient: 'from-slate-500 to-slate-700' };
  
  const initials = text.substring(0, 2).toUpperCase();
  
  const gradients = [
    'from-pink-500 to-rose-600',
    'from-purple-500 to-indigo-600',
    'from-blue-500 to-sky-600',
    'from-emerald-500 to-teal-600',
    'from-amber-500 to-orange-600',
  ];
  
  // Hash text to choose a gradient
  let hash = 0;
  for (let i = 0; i < text.length; i++) {
    hash = text.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % gradients.length;
  
  return {
    initials,
    gradient: gradients[index],
  };
}

export function formatUserLabel(value = '', userDirectory = {}) {
  const raw = String(value || '').trim();
  if (!raw) return 'Unassigned';

  const directoryMatch = userDirectory[raw];
  if (directoryMatch) {
    return String(directoryMatch).split('@')[0] || raw;
  }

  const emailLocalPart = raw.split('@')[0];
  return raw.includes('@') ? emailLocalPart || raw : raw;
}

export function Sidebar({ 
  projects, 
  selectedProjectId, 
  setSelectedProjectId, 
  activeTab, 
  setActiveTab, 
  session, 
  handleLogout, 
  onCreateProjectClick 
}) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const activeProject = projects.find(p => p.id === selectedProjectId) || null;
  const userEmail = session?.email || 'User';
  const avatar = getAvatarDetails(userEmail);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'board', label: 'Kanban Board', icon: KanbanSquare },
    { id: 'sprints', label: 'Sprint Planner', icon: CalendarRange },
    { id: 'settings', label: 'Project Settings', icon: Settings, disabled: !selectedProjectId },
  ];

  return (
    <aside className="flex h-[calc(100vh-2rem)] w-64 flex-col rounded-3xl border border-white/10 bg-slate-900/60 p-4 shadow-xl backdrop-blur-md sticky top-4">
      {/* Brand Logo */}
      <div className="flex items-center gap-2.5 px-3 py-4">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-500 to-purple-600 text-white font-display font-bold text-lg shadow-md shadow-indigo-500/20">
          J
        </div>
        <div>
          <span className="font-display text-lg font-bold text-white tracking-wide">Jirops</span>
          <span className="block text-[10px] uppercase tracking-[0.2em] text-indigo-400 font-semibold mt-[-2px]">Control Center</span>
        </div>
      </div>

      {/* Project Selector Dropdown */}
      <div className="relative mt-4 mb-6" ref={dropdownRef}>
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="flex w-full items-center justify-between gap-3 rounded-2xl border border-white/10 bg-slate-950/40 px-4 py-3 text-left transition hover:bg-slate-950/60"
        >
          <div className="min-w-0 flex-1">
            <div className="truncate text-sm font-semibold text-white">
              {activeProject ? activeProject.name : 'All Projects'}
            </div>
            <div className="text-[10px] uppercase tracking-wider text-slate-400 mt-0.5">
              {activeProject ? activeProject.key : 'SELECT PROJECT'}
            </div>
          </div>
          <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-250 ${dropdownOpen ? 'rotate-180' : ''}`} />
        </button>

        {dropdownOpen && (
          <div className="absolute left-0 right-0 z-50 mt-2 max-h-60 overflow-y-auto rounded-2xl border border-white/10 bg-slate-900 p-2 shadow-2xl animate-fade-in">
            <div className="space-y-1">
              <button
                onClick={() => {
                  setSelectedProjectId('');
                  setDropdownOpen(false);
                }}
                className={`flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-medium transition ${
                  !selectedProjectId 
                    ? 'bg-indigo-600 text-white' 
                    : 'text-slate-300 hover:bg-white/5 hover:text-white'
                }`}
              >
                <Briefcase className="h-4 w-4" />
                All Projects
              </button>

              {projects.map((project) => (
                <button
                  key={project.id}
                  onClick={() => {
                    setSelectedProjectId(project.id);
                    setDropdownOpen(false);
                  }}
                  className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-medium transition ${
                    selectedProjectId === project.id
                      ? 'bg-indigo-600 text-white'
                      : 'text-slate-300 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <div className="truncate flex items-center gap-2.5">
                    <div className="h-2 w-2 rounded-full bg-indigo-400" />
                    <span className="truncate">{project.name}</span>
                  </div>
                  <span className="text-[9px] bg-white/10 text-slate-300 px-1.5 py-0.5 rounded uppercase">
                    {project.key}
                  </span>
                </button>
              ))}

              <div className="border-t border-white/5 my-1.5 pt-1.5">
                <button
                  onClick={() => {
                    onCreateProjectClick();
                    setDropdownOpen(false);
                  }}
                  className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-semibold text-indigo-400 hover:bg-indigo-500/10 transition"
                >
                  <Plus className="h-4 w-4" />
                  New Project
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Nav Links */}
      <nav className="flex-1 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => !item.disabled && setActiveTab(item.id)}
              disabled={item.disabled}
              className={`flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-sm font-semibold transition ${
                item.disabled
                  ? 'opacity-40 cursor-not-allowed'
                  : isActive
                    ? 'bg-gradient-to-r from-indigo-500/20 to-purple-500/10 text-white border-l-2 border-indigo-500'
                    : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'
              }`}
            >
              <Icon className="h-5 w-5" />
              {item.label}
            </button>
          );
        })}
      </nav>

      {/* User Profile Block */}
      <div className="mt-auto border-t border-white/10 pt-4">
        <div className="flex items-center gap-3 px-2">
          <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br ${avatar.gradient} text-white font-bold text-sm shadow`}>
            {avatar.initials}
          </div>
          <div className="min-w-0 flex-1">
            <div className="truncate text-xs font-bold text-white leading-tight">{userEmail}</div>
            <span className="text-[10px] text-slate-400">Collaborator</span>
          </div>
          <button
            onClick={handleLogout}
            title="Sign Out"
            className="rounded-xl p-2 text-slate-400 hover:bg-rose-500/10 hover:text-rose-400 transition"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
