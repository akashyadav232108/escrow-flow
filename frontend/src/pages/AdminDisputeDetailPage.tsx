import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import type { DisputeDetail, DisputeResolution } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

function formatMoney(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value);
}

export default function AdminDisputeDetailPage() {
  const { id } = useParams();
  const disputeId = Number(id);
  const [dispute, setDispute] = useState<DisputeDetail | null>(null);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(disputeId)) {
      setError('Invalid dispute id');
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.getDispute(disputeId);
        if (!cancelled) setDispute(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load dispute'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [disputeId]);

  const handleResolve = async (decision: DisputeResolution) => {
    setBusy(true);
    setError(null);
    try {
      const updated = await adminApi.resolveDispute(disputeId, decision, note.trim() || undefined);
      setDispute(updated);
      setNote('');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to resolve dispute'));
    } finally {
      setBusy(false);
    }
  };

  const onSubmit = (event: FormEvent, decision: DisputeResolution) => {
    event.preventDefault();
    void handleResolve(decision);
  };

  if (loading) return <p>Loading dispute…</p>;
  if (!dispute) return <p className="error-text">{error ?? 'Dispute not found'}</p>;

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>Dispute #{dispute.id}</h1>
          <p className="dashboard-subtitle">
            <Link to="/admin/disputes">← Back to disputes</Link>
          </p>
        </div>
        <span className={`status-pill status-${dispute.status.toLowerCase()}`}>{dispute.status}</span>
      </div>

      {error && <p className="error-text">{error}</p>}

      <section className="admin-detail-card">
        <h2>{dispute.projectTitle}</h2>
        <dl className="admin-dl">
          <div>
            <dt>Milestone</dt>
            <dd>
              {dispute.milestoneTitle} · {formatMoney(dispute.amount)}
            </dd>
          </div>
          <div>
            <dt>Client</dt>
            <dd>{dispute.clientName}</dd>
          </div>
          <div>
            <dt>Freelancer</dt>
            <dd>{dispute.freelancerName ?? '—'}</dd>
          </div>
          <div>
            <dt>Raised by</dt>
            <dd>{dispute.raisedByName}</dd>
          </div>
          <div>
            <dt>Escrow</dt>
            <dd>{dispute.escrowHoldStatus ?? '—'}</dd>
          </div>
          <div>
            <dt>Submitted work</dt>
            <dd>{dispute.submittedNote || '—'}</dd>
          </div>
          <div>
            <dt>Dispute reason</dt>
            <dd>{dispute.reason}</dd>
          </div>
          {dispute.status === 'RESOLVED' && (
            <>
              <div>
                <dt>Resolution</dt>
                <dd>{dispute.resolution}</dd>
              </div>
              <div>
                <dt>Resolved by</dt>
                <dd>{dispute.resolvedByAdminName ?? '—'}</dd>
              </div>
              <div>
                <dt>Admin note</dt>
                <dd>{dispute.adminNote || '—'}</dd>
              </div>
            </>
          )}
        </dl>
      </section>

      {dispute.status === 'OPEN' && (
        <section className="admin-detail-card">
          <h2>Resolve</h2>
          <p className="muted-text">
            Paying the freelancer releases escrow. Refunding the client returns the held amount.
          </p>
          <form className="admin-resolve-form">
            <label>
              Admin note (optional)
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={3}
                placeholder="Explain your decision"
              />
            </label>
            <div className="admin-resolve-actions">
              <button
                type="button"
                className="btn-success"
                disabled={busy}
                onClick={(e) => onSubmit(e, 'FREELANCER_WINS')}
              >
                {busy ? 'Working…' : 'Pay freelancer'}
              </button>
              <button
                type="button"
                className="btn-danger-outline"
                disabled={busy}
                onClick={(e) => onSubmit(e, 'CLIENT_WINS')}
              >
                {busy ? 'Working…' : 'Refund client'}
              </button>
            </div>
          </form>
        </section>
      )}
    </div>
  );
}
