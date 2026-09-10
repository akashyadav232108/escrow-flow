import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { adminProjectExitApi } from '../api/projectExitApi';
import type { ProjectExitDetail, ProjectExitOutcome } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

function formatMoney(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value);
}

export default function AdminProjectExitDetailPage() {
  const { id } = useParams();
  const exitId = Number(id);
  const [exit, setExit] = useState<ProjectExitDetail | null>(null);
  const [note, setNote] = useState('');
  const [amounts, setAmounts] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(exitId)) {
      setError('Invalid exit id');
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminProjectExitApi.getById(exitId);
        if (cancelled) return;
        setExit(data);
        const initial: Record<number, string> = {};
        for (const s of data.settlements) {
          initial[s.milestoneId] =
            s.freelancerAmount != null ? String(s.freelancerAmount) : '0';
        }
        setAmounts(initial);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load project exit'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [exitId]);

  const canResolve = exit?.status === 'OPEN';

  const settlementPreview = useMemo(() => {
    if (!exit) return [];
    return exit.settlements.map((s) => {
      const raw = amounts[s.milestoneId] ?? '0';
      const freelancerAmount = Number(raw);
      const hold = Number(s.holdAmount);
      const valid = Number.isFinite(freelancerAmount) && freelancerAmount >= 0 && freelancerAmount <= hold;
      const clientRefund = valid ? hold - freelancerAmount : NaN;
      return { ...s, freelancerAmount, clientRefund, valid, hold };
    });
  }, [exit, amounts]);

  const handleResolve = async (outcome: ProjectExitOutcome) => {
    if (!exit) return;
    for (const row of settlementPreview) {
      if (!row.valid) {
        setError(`Invalid freelancer amount for milestone "${row.milestoneTitle}"`);
        return;
      }
    }
    setBusy(true);
    setError(null);
    try {
      const updated = await adminProjectExitApi.resolve(exitId, {
        projectOutcome: outcome,
        adminNote: note.trim() || undefined,
        settlements: settlementPreview.map((row) => ({
          milestoneId: row.milestoneId,
          freelancerAmount: row.freelancerAmount,
        })),
      });
      setExit(updated);
      setNote('');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to resolve project exit'));
    } finally {
      setBusy(false);
    }
  };

  const onSubmit = (event: FormEvent, outcome: ProjectExitOutcome) => {
    event.preventDefault();
    void handleResolve(outcome);
  };

  if (loading) return <p>Loading project exit…</p>;
  if (!exit) return <p className="error-text">{error ?? 'Project exit not found'}</p>;

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>Project exit #{exit.id}</h1>
          <p className="dashboard-subtitle">
            <Link to="/admin/project-exits">← Back to project exits</Link>
          </p>
        </div>
        <span className={`status-pill status-${exit.status.toLowerCase()}`}>{exit.status}</span>
      </div>

      {error && <p className="error-text">{error}</p>}

      <section className="admin-detail-card">
        <h2>{exit.projectTitle}</h2>
        <dl className="admin-dl">
          <div>
            <dt>Client</dt>
            <dd>{exit.clientName}</dd>
          </div>
          <div>
            <dt>Freelancer</dt>
            <dd>{exit.freelancerName ?? '—'}</dd>
          </div>
          <div>
            <dt>Raised by</dt>
            <dd>{exit.raisedByName}</dd>
          </div>
          <div>
            <dt>Reason</dt>
            <dd>{exit.reason}</dd>
          </div>
          {exit.projectOutcome && (
            <div>
              <dt>Outcome</dt>
              <dd>{exit.projectOutcome}</dd>
            </div>
          )}
          {exit.adminNote && (
            <div>
              <dt>Admin note</dt>
              <dd>{exit.adminNote}</dd>
            </div>
          )}
        </dl>
      </section>

      <section className="admin-detail-card">
        <h2>Held milestones</h2>
        {exit.settlements.length === 0 && (
          <p className="muted-text">No escrow held — resolve by cancelling or reopening only.</p>
        )}
        {exit.settlements.length > 0 && (
          <ul className="exit-admin-settlements">
            {settlementPreview.map((row) => (
              <li key={row.milestoneId}>
                <div className="exit-admin-settlement-head">
                  <strong>{row.milestoneTitle}</strong>
                  <span className="muted-text">Hold {formatMoney(row.hold)}</span>
                </div>
                {canResolve ? (
                  <label className="exit-amount-label">
                    Amount to freelancer
                    <input
                      type="number"
                      min={0}
                      max={row.hold}
                      step="0.0001"
                      value={amounts[row.milestoneId] ?? '0'}
                      onChange={(e) =>
                        setAmounts((prev) => ({ ...prev, [row.milestoneId]: e.target.value }))
                      }
                    />
                  </label>
                ) : (
                  <p className="muted-text">
                    Freelancer {formatMoney(Number(row.freelancerAmount ?? 0))} · Client refund{' '}
                    {formatMoney(Number(row.clientRefundAmount ?? 0))}
                  </p>
                )}
                {canResolve && row.valid && (
                  <p className="muted-text">
                    Client refund: {formatMoney(row.clientRefund)} (
                    {row.hold === 0 ? 0 : Math.round((row.freelancerAmount / row.hold) * 100)}% to
                    freelancer)
                  </p>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      {canResolve && (
        <form className="admin-resolve-form">
          <label>
            Admin note (optional)
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              maxLength={5000}
            />
          </label>
          <div className="application-actions">
            <button
              type="button"
              className="btn-primary"
              disabled={busy}
              onClick={(e) => onSubmit(e, 'CANCELLED')}
            >
              {busy ? 'Working…' : 'Settle & cancel project'}
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={(e) => onSubmit(e, 'REOPEN')}
            >
              {busy ? 'Working…' : 'Settle & reopen for hiring'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
