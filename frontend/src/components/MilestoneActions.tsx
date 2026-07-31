import { useState } from 'react';
import type { Milestone } from '../types';
import { useAppDispatch } from '../store/hooks';
import {
  approveMilestone,
  disputeMilestone,
  fetchProjectById,
  lockFunds,
  submitMilestone,
} from '../store/slices/projectsSlice';
import { fetchWallet } from '../store/slices/walletSlice';
import { createIdempotencyKey } from '../utils/idempotency';
import { extractApiErrorMessage } from '../utils/errors';

interface MilestoneActionsProps {
  milestone: Milestone;
  projectId: number;
  isClient: boolean;
  isFreelancer: boolean;
}

export default function MilestoneActions({
  milestone,
  projectId,
  isClient,
  isFreelancer,
}: MilestoneActionsProps) {
  const dispatch = useAppDispatch();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState<string | null>(null);
  const [showSubmitForm, setShowSubmitForm] = useState(false);
  const [showDisputeForm, setShowDisputeForm] = useState(false);
  const [note, setNote] = useState('');
  const [disputeReason, setDisputeReason] = useState('');

  const refresh = async () => {
    await Promise.all([dispatch(fetchProjectById(projectId)), dispatch(fetchWallet())]);
  };

  const handleLockFunds = async () => {
    setBusy(true);
    setError(null);
    const key = idempotencyKey ?? createIdempotencyKey();
    setIdempotencyKey(key);
    try {
      await dispatch(lockFunds({ milestoneId: milestone.id, idempotencyKey: key })).unwrap();
      setIdempotencyKey(null);
      await refresh();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to lock funds'));
    } finally {
      setBusy(false);
    }
  };

  const handleSubmitWork = async () => {
    if (!note.trim()) {
      setError('Describe the work you delivered before submitting.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await dispatch(submitMilestone({ milestoneId: milestone.id, note: note.trim() })).unwrap();
      setNote('');
      setShowSubmitForm(false);
      await refresh();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to submit work'));
    } finally {
      setBusy(false);
    }
  };

  const handleApprove = async () => {
    setBusy(true);
    setError(null);
    try {
      await dispatch(approveMilestone(milestone.id)).unwrap();
      await refresh();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to approve milestone'));
    } finally {
      setBusy(false);
    }
  };

  const handleDispute = async () => {
    if (!disputeReason.trim()) {
      setError('Please provide a reason for the dispute.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await dispatch(
        disputeMilestone({ milestoneId: milestone.id, reason: disputeReason.trim() }),
      ).unwrap();
      setDisputeReason('');
      setShowDisputeForm(false);
      await refresh();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to dispute milestone'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="milestone-actions">
      {isClient && milestone.status === 'PENDING' && (
        <button type="button" className="btn-soft btn-sm" disabled={busy} onClick={handleLockFunds}>
          Lock funds
        </button>
      )}

      {isFreelancer && milestone.status === 'FUNDS_LOCKED' && !showSubmitForm && (
        <button
          type="button"
          className="btn-soft btn-sm"
          disabled={busy}
          onClick={() => setShowSubmitForm(true)}
        >
          Submit work
        </button>
      )}

      {isFreelancer && showSubmitForm && (
        <div className="inline-action-form">
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Describe the delivered work, links, etc."
            rows={2}
            autoFocus
          />
          <div className="inline-action-buttons">
            <button type="button" className="btn-primary" disabled={busy} onClick={handleSubmitWork}>
              {busy ? 'Submitting…' : 'Confirm submit'}
            </button>
            <button
              type="button"
              className="btn-ghost"
              disabled={busy}
              onClick={() => {
                setShowSubmitForm(false);
                setNote('');
                setError(null);
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {isClient && milestone.status === 'SUBMITTED' && !showDisputeForm && (
        <>
          <button type="button" className="btn-success btn-sm" disabled={busy} onClick={handleApprove}>
            {busy ? 'Approving…' : 'Approve'}
          </button>
          <button
            type="button"
            className="btn-danger-outline btn-sm"
            disabled={busy}
            onClick={() => setShowDisputeForm(true)}
          >
            Dispute
          </button>
        </>
      )}

      {isFreelancer && milestone.status === 'SUBMITTED' && !showDisputeForm && (
        <button
          type="button"
          className="btn-danger-outline btn-sm"
          disabled={busy}
          onClick={() => setShowDisputeForm(true)}
        >
          Dispute
        </button>
      )}

      {(isClient || isFreelancer) && showDisputeForm && (
        <div className="inline-action-form">
          <textarea
            value={disputeReason}
            onChange={(e) => setDisputeReason(e.target.value)}
            placeholder="Reason for dispute (required)"
            rows={2}
            autoFocus
            required
          />
          <div className="inline-action-buttons">
            <button type="button" className="btn-danger-outline" disabled={busy} onClick={handleDispute}>
              {busy ? 'Submitting…' : 'Confirm dispute'}
            </button>
            <button
              type="button"
              className="btn-ghost"
              disabled={busy}
              onClick={() => {
                setShowDisputeForm(false);
                setDisputeReason('');
                setError(null);
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {milestone.status === 'DISPUTED' && (
        <p className="muted-text">Under admin review — funds remain in escrow until resolved.</p>
      )}

      {error && <p className="error-text">{error}</p>}
    </div>
  );
}
