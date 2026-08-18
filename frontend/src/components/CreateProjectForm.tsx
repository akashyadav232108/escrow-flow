import { useEffect, useState, type FormEvent } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { createProject } from '../store/slices/projectsSlice';
import { extractApiErrorMessage } from '../utils/errors';
import {
  clearProjectDraft,
  isProjectDraftDirty,
  loadProjectDraft,
  saveProjectDraft,
} from '../utils/projectDraft';

interface MilestoneRow {
  title: string;
  description: string;
  amount: string;
}

const emptyRow = (): MilestoneRow => ({ title: '', description: '', amount: '' });

export default function CreateProjectForm({ onCreated }: { onCreated: () => void }) {
  const dispatch = useAppDispatch();
  const loading = useAppSelector((state) => state.projects.loading);
  const userId = useAppSelector((state) => state.auth.user?.id);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [milestones, setMilestones] = useState<MilestoneRow[]>([emptyRow()]);
  const [error, setError] = useState<string | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [restoredDraft, setRestoredDraft] = useState(false);

  useEffect(() => {
    if (!userId) return;
    const draft = loadProjectDraft(userId);
    if (draft) {
      setTitle(draft.title);
      setDescription(draft.description);
      setMilestones(draft.milestones.length > 0 ? draft.milestones : [emptyRow()]);
      setRestoredDraft(isProjectDraftDirty(draft));
    }
    setHydrated(true);
  }, [userId]);

  useEffect(() => {
    if (!hydrated || !userId) return;

    const timeoutId = window.setTimeout(() => {
      const draft = { title, description, milestones };
      if (!isProjectDraftDirty(draft)) {
        clearProjectDraft(userId);
        return;
      }
      saveProjectDraft(userId, draft);
    }, 400);

    return () => window.clearTimeout(timeoutId);
  }, [hydrated, userId, title, description, milestones]);

  useEffect(() => {
    if (!hydrated || !userId) return;
    const dirty = isProjectDraftDirty({ title, description, milestones });
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (dirty) {
        event.preventDefault();
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hydrated, userId, title, description, milestones]);

  const updateMilestone = (index: number, field: keyof MilestoneRow, value: string) => {
    setMilestones((prev) => prev.map((row, i) => (i === index ? { ...row, [field]: value } : row)));
  };

  const addMilestone = () => setMilestones((prev) => [...prev, emptyRow()]);

  const removeMilestone = (index: number) =>
    setMilestones((prev) => (prev.length > 1 ? prev.filter((_, i) => i !== index) : prev));

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    const payloadMilestones = milestones
      .filter((row) => row.title.trim() && row.amount)
      .map((row) => ({
        title: row.title.trim(),
        description: row.description.trim() || undefined,
        amount: Number(row.amount),
      }));

    if (payloadMilestones.length === 0) {
      setError('Add at least one milestone with a title and amount.');
      return;
    }

    try {
      await dispatch(
        createProject({ title, description: description.trim() || undefined, milestones: payloadMilestones }),
      ).unwrap();
      if (userId) clearProjectDraft(userId);
      setTitle('');
      setDescription('');
      setMilestones([emptyRow()]);
      setRestoredDraft(false);
      onCreated();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to create project'));
    }
  };

  return (
    <form className="create-project-form" onSubmit={handleSubmit}>
      <h2>New project</h2>
      {restoredDraft && (
        <p className="form-hint">Your unsaved draft was restored. It will be kept until you create the project.</p>
      )}
      <label>
        Title
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Website redesign"
          required
        />
      </label>
      <label>
        Description
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Brief summary of the work (optional)"
          rows={2}
        />
      </label>

      <div className="milestone-rows">
        <span className="milestone-rows-label">Milestones</span>
        {milestones.map((row, index) => (
          <div className="milestone-row-input" key={index}>
            <input
              value={row.title}
              onChange={(e) => updateMilestone(index, 'title', e.target.value)}
              placeholder="Milestone title"
              required
            />
            <input
              value={row.description}
              onChange={(e) => updateMilestone(index, 'description', e.target.value)}
              placeholder="Description (optional)"
            />
            <input
              type="number"
              min="1"
              step="0.01"
              value={row.amount}
              onChange={(e) => updateMilestone(index, 'amount', e.target.value)}
              placeholder="Amount"
              required
            />
            <button
              type="button"
              className="btn-icon-remove"
              onClick={() => removeMilestone(index)}
              disabled={milestones.length === 1}
              aria-label="Remove milestone"
            >
              ×
            </button>
          </div>
        ))}
        <button type="button" className="btn-add-milestone" onClick={addMilestone}>
          + Add milestone
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}
      <button type="submit" className="btn-primary" disabled={loading}>
        {loading ? 'Creating…' : 'Create project'}
      </button>
    </form>
  );
}
