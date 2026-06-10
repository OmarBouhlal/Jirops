import { useState } from 'react';
import { 
  Settings, 
  Users, 
  Edit3, 
  Calendar,
  Key,
  AlertTriangle
} from 'lucide-react';
import { formatUserLabel, getAvatarDetails } from './Sidebar';

function formatDateTime(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (isNaN(date.getTime())) return 'Not set';
  return new Intl.DateTimeFormat('en', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function ProjectSettingsView({ 
  activeProject, 
  users,
  usersLoading,
  userDirectory,
  onUpdateClick, 
  onDeleteClick, 
  onAddMember
}) {
  const [memberSearch, setMemberSearch] = useState('');

  if (!activeProject) {
    return (
      <div className="rounded-3xl border border-dashed border-white/10 bg-white/5 p-8 text-center text-slate-400">
        Please select a project to configure settings.
      </div>
    );
  }

  const ownerAvatar = getAvatarDetails(activeProject.ownerId || 'Owner');
  const activeMemberIds = new Set([activeProject.ownerId, ...(activeProject.members || [])]);
  const query = memberSearch.trim().toLowerCase();
  const availableUsers = (users || [])
    .map((user) => ({
      id: user?.id || user?._id || '',
      email: user?.email || '',
    }))
    .filter((user) => user.id)
    .filter((user) => !activeMemberIds.has(user.id))
    .filter((user) => {
      if (!query) return true;
      return user.email.toLowerCase().includes(query) || user.id.toLowerCase().includes(query);
    });

  async function handleInvite(userId) {
    await onAddMember(userId);
    setMemberSearch('');
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="border-b border-white/10 pb-4 animate-fade-in">
        <h3 className="font-display text-xl font-bold text-white flex items-center gap-2">
          <Settings className="h-5 w-5 text-indigo-400" />
          Project Settings
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">Configure project parameters and team collaborations.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Project Details Panel */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass rounded-3xl p-6 shadow-glow space-y-6">
            <div className="flex items-start justify-between">
              <div>
                <span className="rounded-md bg-indigo-500/10 border border-indigo-500/20 px-2 py-0.5 text-[10px] font-bold text-indigo-400 uppercase tracking-wider">
                  Project Workspace
                </span>
                <h4 className="mt-2 text-2xl font-bold text-white">{activeProject.name}</h4>
                {activeProject.description && (
                  <p className="mt-2 text-sm leading-relaxed text-slate-300">
                    {activeProject.description}
                  </p>
                )}
              </div>
              <button
                onClick={onUpdateClick}
                className="rounded-xl border border-white/10 bg-white/5 p-2 text-slate-300 hover:bg-white/10 hover:text-white transition"
                title="Edit Project Details"
              >
                <Edit3 className="h-4.5 w-4.5" />
              </button>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 border-t border-white/5 pt-5 text-xs text-slate-300">
              <div className="flex items-center gap-3 rounded-2xl bg-white/5 p-3.5 border border-white/5">
                <Key className="h-4 w-4 text-slate-400" />
                <div>
                  <span className="block text-[10px] uppercase text-slate-400 tracking-wider">Project Key</span>
                  <span className="font-semibold text-white">{activeProject.key}</span>
                </div>
              </div>

              <div className="flex items-center gap-3 rounded-2xl bg-white/5 p-3.5 border border-white/5">
                <Calendar className="h-4 w-4 text-slate-400" />
                <div>
                  <span className="block text-[10px] uppercase text-slate-400 tracking-wider">Created At</span>
                  <span className="font-semibold text-white">{formatDateTime(activeProject.createdAt)}</span>
                </div>
              </div>
            </div>

            {/* Owner details */}
            <div className="border-t border-white/5 pt-5">
              <span className="block text-[10px] uppercase text-slate-400 tracking-wider mb-2.5">Project Owner</span>
              <div className="flex items-center gap-3 rounded-2xl bg-white/5 p-3.5 border border-white/5">
                <div className={`h-9 w-9 flex items-center justify-center rounded-2xl bg-gradient-to-br ${ownerAvatar.gradient} text-white font-bold text-xs`}>
                  {ownerAvatar.initials}
                </div>
                <div>
                  <span className="block text-xs font-semibold text-white truncate max-w-[250px]">{formatUserLabel(activeProject.ownerId, userDirectory)}</span>
                  <span className="text-[10px] text-slate-400">Creator & Root Administrator</span>
                </div>
              </div>
            </div>
          </div>

          {/* Danger Zone */}
          <div className="rounded-3xl border border-rose-500/10 bg-rose-500/5 p-6 space-y-4">
            <div>
              <h4 className="text-base font-bold text-rose-400 flex items-center gap-2">
                <AlertTriangle className="h-5 w-5" />
                Danger Zone
              </h4>
              <p className="text-xs text-slate-400 mt-1">
                Once you delete a project, it cannot be undone. All tasks and sprints associated with it will be permanently lost.
              </p>
            </div>
            
            <button
              onClick={onDeleteClick}
              className="rounded-2xl bg-rose-600 hover:bg-rose-500 px-5 py-2.5 text-xs font-bold text-white shadow-md shadow-rose-600/15 transition-all"
            >
              Delete Project
            </button>
          </div>
        </div>

        {/* Project Members Panel */}
        <div className="glass rounded-3xl p-6 shadow-glow flex flex-col max-h-[500px]">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
            <Users className="h-4.5 w-4.5 text-indigo-400" />
            Project Team
          </h3>

          {/* Add member form */}
          <div className="space-y-2 mb-4">
            <label className="block">
              <span className="mb-1.5 block text-[10px] uppercase tracking-wider text-slate-400">
                Search users
              </span>
              <input
                type="search"
                value={memberSearch}
                onChange={(e) => setMemberSearch(e.target.value)}
                placeholder="Search by email or user id"
                className="w-full rounded-2xl border border-white/10 bg-slate-950/40 px-3 py-2.5 text-xs text-white outline-none transition placeholder:text-slate-600 focus:border-indigo-500/50"
              />
            </label>
            <span className="block text-[9px] text-slate-400 leading-normal">
              Search the user directory, then invite the people you want to add to this project.
            </span>
          </div>

          <div className="mb-4 flex items-center justify-between rounded-2xl border border-white/5 bg-white/5 px-3 py-2 text-[10px] uppercase tracking-wider text-slate-400">
            <span>
              {usersLoading ? 'Loading users...' : `${availableUsers.length} available user${availableUsers.length === 1 ? '' : 's'}`}
            </span>
            <span>{memberSearch.trim() ? 'Filtered results' : 'All users'}</span>
          </div>

          {/* Members List */}
          <div className="space-y-3 overflow-y-auto pr-1 flex-1">
            {usersLoading ? (
              <p className="text-xs text-slate-500 text-center py-4">Loading available users...</p>
            ) : availableUsers.length > 0 ? (
              availableUsers.map((user) => {
                const memberLabel = formatUserLabel(user.id || user.email, userDirectory);
                const memberAvatar = getAvatarDetails(memberLabel);
                return (
                  <div
                    key={user.id}
                    className="flex items-center gap-3 rounded-2xl border border-white/5 bg-slate-900/40 p-3"
                  >
                    <div className={`h-8 w-8 flex items-center justify-center rounded-xl bg-gradient-to-br ${memberAvatar.gradient} text-white font-bold text-[10px]`}>
                      {memberAvatar.initials}
                    </div>
                    <div className="min-w-0 flex-1">
                      <span className="block text-xs font-semibold text-white truncate">{memberLabel}</span>
                      <span className="block text-[9px] text-slate-400 truncate">{user.email || user.id}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleInvite(user.id)}
                      className="rounded-xl bg-indigo-600 px-3 py-1.5 text-[10px] font-bold text-white hover:bg-indigo-500 transition"
                    >
                      Invite
                    </button>
                  </div>
                );
              })
            ) : (
              <p className="text-xs text-slate-500 text-center py-4">No matching users found.</p>
            )}

            {activeProject.members && activeProject.members.length > 0 ? (
              <>
                <div className="pt-2">
                  <span className="block text-[10px] uppercase tracking-wider text-slate-400 mb-2">Current members</span>
                  <div className="space-y-3">
                    {activeProject.members.map((memberId) => {
                      const memberLabel = formatUserLabel(memberId, userDirectory);
                      const memberAvatar = getAvatarDetails(memberLabel);
                      return (
                        <div
                          key={memberId}
                          className="flex items-center gap-3 rounded-2xl border border-white/5 bg-slate-900/40 p-3"
                        >
                          <div className={`h-8 w-8 flex items-center justify-center rounded-xl bg-gradient-to-br ${memberAvatar.gradient} text-white font-bold text-[10px]`}>
                            {memberAvatar.initials}
                          </div>
                          <div className="min-w-0 flex-1">
                            <span className="block text-xs font-semibold text-white truncate">{memberLabel}</span>
                            <span className="block text-[9px] text-slate-400 uppercase tracking-wider mt-0.5">Developer</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </>
            ) : (
              <p className="text-xs text-slate-500 text-center py-4">No other members in this team.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
