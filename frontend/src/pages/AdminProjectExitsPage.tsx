import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminProjectExitApi } from '../api/projectExitApi';
import type { ProjectExitDetail, ProjectExitStatus } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

export default function AdminProjectExitsPage() {
  const [items, setItems] = useState<ProjectExitDetail[]>([]);
  const [status, setStatus] = useState<ProjectExitStatus | ''>('OPEN');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminProjectExitApi.list(status || undefined);
        if (!cancelled) setItems(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load project exits'));
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
          <h1>Project exits</h1>
          <p className="dashboard-subtitle">Settle held escrow per milestone; decision is final.</p>
        </div>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as ProjectExitStatus | '')}
          aria-label="Filter by status"
        >
          <option value="">All</option>
          <option value="OPEN">OPEN</option>
          <option value="RESOLVED">RESOLVED</option>
        </select>
      </div>

      {error && <p className="error-text">{error}</p>}
      {loading && <p>Loading…</p>}

      {!loading && items.length === 0 && <p className="empty-state">No project exits found.</p>}

      {!loading && items.length > 0 && (
        <div className="admin-list">
          {items.map((item) => (
            <Link key={item.id} to={`/admin/project-exits/${item.id}`} className="admin-list-item">
              <div>
                <div className="admin-list-title">
                  #{item.id} · {item.projectTitle}
                </div>
                <div className="admin-list-meta">
                  {item.clientName} / {item.freelancerName ?? '—'} · {item.settlements.length} held
                  milestone{item.settlements.length === 1 ? '' : 's'}
                </div>
              </div>
              <span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
