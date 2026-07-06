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
      setError(err instanceof Error ? err.message : 'Failed to lock funds');
    } finally {
      setBusy(false);
    }
  };

  const handleSubmit = async () => {
    const note = window.prompt('Describe the delivered work:');
    if (note === null) return;
    setBusy(true);
    setError(null);
    try {
      await dispatch(submitMilestone({ milestoneId: milestone.id, note })).unwrap();
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit work');
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
      setError(err instanceof Error ? err.message : 'Failed to approve milestone');
    } finally {
      setBusy(false);
    }
  };

  const handleDispute = async () => {
    const reason = window.prompt('Reason for dispute (optional):') ?? undefined;
    setBusy(true);
    setError(null);
    try {
      await dispatch(disputeMilestone({ milestoneId: milestone.id, reason })).unwrap();
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to dispute milestone');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="milestone-actions">
      {isClient && milestone.status === 'PENDING' && (
        <button type="button" disabled={busy} onClick={handleLockFunds}>
          Lock funds
        </button>
      )}
      {isFreelancer && milestone.status === 'FUNDS_LOCKED' && (
        <button type="button" disabled={busy} onClick={handleSubmit}>
          Submit work
        </button>
      )}
      {isClient && milestone.status === 'SUBMITTED' && (
        <>
          <button type="button" disabled={busy} onClick={handleApprove}>
            Approve
          </button>
          <button type="button" disabled={busy} onClick={handleDispute}>
            Dispute
          </button>
        </>
      )}
      {error && <p className="error-text">{error}</p>}
    </div>
  );
}
