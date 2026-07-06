import { useState, type FormEvent } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { createProject } from '../store/slices/projectsSlice';
import { extractApiErrorMessage } from '../utils/errors';

interface MilestoneRow {
  title: string;
  description: string;
  amount: string;
}

const emptyRow = (): MilestoneRow => ({ title: '', description: '', amount: '' });

export default function CreateProjectForm({ onCreated }: { onCreated: () => void }) {
  const dispatch = useAppDispatch();
  const loading = useAppSelector((state) => state.projects.loading);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [milestones, setMilestones] = useState<MilestoneRow[]>([emptyRow()]);
  const [error, setError] = useState<string | null>(null);

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
      setTitle('');
      setDescription('');
      setMilestones([emptyRow()]);
      onCreated();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to create project'));
    }
  };

  return (
    <form className="create-project-form" onSubmit={handleSubmit}>
      <h2>New project</h2>
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
      <button type="submit" disabled={loading}>
        {loading ? 'Creating…' : 'Create project'}
      </button>
    </form>
  );
}
