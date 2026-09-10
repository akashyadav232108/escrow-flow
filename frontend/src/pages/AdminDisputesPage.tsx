import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import type { DisputeDetail, DisputeStatus } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

function formatMoney(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value);
}

export default function AdminDisputesPage() {
  const [status, setStatus] = useState<DisputeStatus | 'ALL'>('OPEN');
  const [disputes, setDisputes] = useState<DisputeDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.listDisputes(status === 'ALL' ? undefined : status);
        if (!cancelled) setDisputes(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load disputes'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status]);

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>Disputes</h1>
          <p className="dashboard-subtitle">Review frozen escrow and decide payouts.</p>
        </div>
        <div className="admin-filter-row">
          {(['OPEN', 'RESOLVED', 'ALL'] as const).map((value) => (
            <button
              key={value}
              type="button"
              className={status === value ? 'btn-soft btn-sm' : 'btn-ghost btn-sm'}
              onClick={() => setStatus(value)}
            >
              {value === 'ALL' ? 'All' : value.charAt(0) + value.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {loading && <p>Loading disputes…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && disputes.length === 0 && (
        <p className="empty-state">No disputes in this view.</p>
      )}

      <div className="admin-list">
        {disputes.map((dispute) => (
          <Link key={dispute.id} to={`/admin/disputes/${dispute.id}`} className="admin-list-item">
            <div>
              <div className="admin-list-title">{dispute.projectTitle}</div>
              <div className="admin-list-meta">
                {dispute.milestoneTitle} · {formatMoney(dispute.amount)} · raised by{' '}
                {dispute.raisedByName}
              </div>
            </div>
            <span className={`status-pill status-${dispute.status.toLowerCase()}`}>
              {dispute.status}
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}
