import { useEffect, useState } from 'react';
import { AlertTriangle, Info, Check, X } from 'lucide-react';

export function ConfirmModal({ isOpen, onClose, onConfirm, title, message, confirmText = 'Confirm', type = 'danger' }) {
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

  const typeStyles = {
    danger: {
      iconBg: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
      btnBg: 'bg-rose-600 hover:bg-rose-500 focus:ring-rose-500/20 text-white',
      borderGlow: 'focus:border-rose-500/30',
    },
    warning: {
      iconBg: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
      btnBg: 'bg-amber-600 hover:bg-amber-500 focus:ring-amber-500/20 text-white',
      borderGlow: 'focus:border-amber-500/30',
    },
    info: {
      iconBg: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
      btnBg: 'bg-indigo-600 hover:bg-indigo-500 focus:ring-indigo-500/20 text-white',
      borderGlow: 'focus:border-indigo-500/30',
    },
  };

  const currentStyles = typeStyles[type] || typeStyles.danger;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
        onClick={onClose}
      />
      
      {/* Modal content */}
      <div className="relative w-full max-w-md overflow-hidden rounded-3xl border border-white/10 bg-slate-900/90 p-6 shadow-2xl backdrop-blur-md animate-scale-up">
        <button 
          onClick={onClose} 
          className="absolute right-4 top-4 rounded-xl p-1.5 text-slate-400 hover:bg-white/5 hover:text-white transition"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex gap-4">
          <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border ${currentStyles.iconBg}`}>
            {type === 'danger' || type === 'warning' ? (
              <AlertTriangle className="h-6 w-6" />
            ) : (
              <Info className="h-6 w-6" />
            )}
          </div>

          <div className="flex-1">
            <h3 className="font-display text-xl font-bold text-white">{title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-slate-300">{message}</p>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white hover:bg-white/10 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className={`rounded-2xl px-5 py-2.5 text-sm font-semibold transition ${currentStyles.btnBg}`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export function EditProjectModal({ isOpen, onClose, onSave, project }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (project) {
      setName(project.name || '');
      setDescription(project.description || '');
    }
  }, [project, isOpen]);

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

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({ name: name.trim(), description: description.trim() });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
        onClick={onClose}
      />
      
      {/* Modal content */}
      <form 
        onSubmit={handleSubmit}
        className="relative w-full max-w-md overflow-hidden rounded-3xl border border-white/10 bg-slate-900/90 p-6 shadow-2xl backdrop-blur-md animate-scale-up"
      >
        <button 
          type="button"
          onClick={onClose} 
          className="absolute right-4 top-4 rounded-xl p-1.5 text-slate-400 hover:bg-white/5 hover:text-white transition"
        >
          <X className="h-5 w-5" />
        </button>

        <h3 className="font-display text-xl font-bold text-white mb-5">Edit Project Details</h3>

        <div className="space-y-4">
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">Project Name</span>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition"
              placeholder="Project Name"
            />
          </label>

          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-200">Description</span>
            <textarea
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3 text-sm text-white outline-none focus:border-indigo-500/50 focus:bg-slate-950/80 transition resize-none"
              placeholder="What is this project about?"
            />
          </label>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-2xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-slate-300 hover:bg-white/10 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            className="rounded-2xl bg-indigo-600 hover:bg-indigo-500 px-5 py-2.5 text-sm font-semibold text-white transition focus:ring-2 focus:ring-indigo-500/20"
          >
            Save Changes
          </button>
        </div>
      </form>
    </div>
  );
}
