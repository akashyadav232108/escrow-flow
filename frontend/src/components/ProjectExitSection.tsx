import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { projectExitApi } from '../api/projectExitApi';
import type { ProjectExitDetail, ProjectStatus } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

interface ProjectExitSectionProps {
  projectId: number;
  projectStatus: ProjectStatus;
  isClient: boolean;
  isAssignedFreelancer: boolean;
  onChanged?: () => void;
}

function formatMoney(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value);
}

export default function ProjectExitSection({
  projectId,
  projectStatus,
  isClient,
  isAssignedFreelancer,
  onChanged,
}: ProjectExitSectionProps) {
  const canRaise = (isClient || isAssignedFreelancer) && projectStatus === 'IN_PROGRESS';
  const showOpenExit = (isClient || isAssignedFreelancer) && projectStatus === 'EXIT_DISPUTED';

  const [exit, setExit] = useState<ProjectExitDetail | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  const loadOpenExit = useCallback(async () => {
    if (!showOpenExit) {
      setExit(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await projectExitApi.getOpenForProject(projectId);
      setExit(data);
    } catch (err) {
      setExit(null);
      setError(extractApiErrorMessage(err, 'Failed to load project exit'));
    } finally {
      setLoading(false);
    }
  }, [projectId, showOpenExit]);

  useEffect(() => {
    void loadOpenExit();
  }, [loadOpenExit]);

  if (!canRaise && !showOpenExit) {
    return null;
  }

  const handleRaise = async (event: FormEvent) => {
    event.preventDefault();
    if (!reason.trim()) {
      setError('Please provide a reason for exiting the project.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await projectExitApi.raise(projectId, reason.trim());
      setExit(created);
      setShowForm(false);
      setReason('');
      onChanged?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to raise project exit'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="applications-section">
      <h2>Project exit</h2>
      <p className="muted-text">
        Request to leave or end the engagement. Held escrow is frozen until an admin settles each
        milestone (release vs refund). Admin decision is final.
      </p>

      {error && <p className="error-text">{error}</p>}
      {loading && <p className="muted-text">Loading…</p>}

      {showOpenExit && exit && (
        <div className="application-mine">
          <span className="status-badge status-disputed">EXIT UNDER REVIEW</span>
          <p className="application-message">{exit.reason}</p>
          <p className="muted-text application-meta">
            Raised by {exit.raisedByName} · {new Date(exit.createdAt).toLocaleString()}
          </p>
          {exit.settlements.length > 0 && (
            <ul className="exit-settlement-list">
              {exit.settlements.map((s) => (
                <li key={s.id}>
                  {s.milestoneTitle}: {formatMoney(Number(s.holdAmount))} held
                </li>
              ))}
            </ul>
          )}
          {exit.settlements.length === 0 && (
            <p className="muted-text">No funds currently held in escrow for this project.</p>
          )}
        </div>
      )}

      {canRaise && !showForm && (
        <button type="button" className="btn-secondary" onClick={() => setShowForm(true)}>
          Request project exit
        </button>
      )}

      {canRaise && showForm && (
        <form className="application-form" onSubmit={(e) => void handleRaise(e)}>
          <label>
            Reason
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={5000}
              rows={3}
              required
              placeholder="Why should this engagement end?"
            />
          </label>
          <div className="application-actions">
            <button type="submit" className="btn-primary" disabled={busy}>
              {busy ? 'Submitting…' : 'Submit exit request'}
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={() => {
                setShowForm(false);
                setReason('');
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
