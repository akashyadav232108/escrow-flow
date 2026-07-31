import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import type { ManagedUser } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

type Action = 'warn' | 'suspend' | 'delete' | null;

export default function AdminUserDetailPage() {
  const { id } = useParams();
  const userId = Number(id);
  const [user, setUser] = useState<ManagedUser | null>(null);
  const [action, setAction] = useState<Action>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    const data = await adminApi.getUser(userId);
    setUser(data);
  };

  useEffect(() => {
    if (!Number.isFinite(userId)) {
      setError('Invalid user id');
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.getUser(userId);
        if (!cancelled) setUser(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load user'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const runAction = async (event: FormEvent) => {
    event.preventDefault();
    if (!action) return;
    if (!reason.trim()) {
      setError('Reason is required.');
      return;
    }
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      if (action === 'warn') await adminApi.warnUser(userId, reason.trim());
      if (action === 'suspend') await adminApi.suspendUser(userId, reason.trim());
      if (action === 'delete') await adminApi.deleteUser(userId, reason.trim());
      await load();
      setReason('');
      setAction(null);
      setSuccess(
        action === 'warn'
          ? 'Warning issued.'
          : action === 'suspend'
            ? 'User suspended.'
            : 'User soft-deleted.',
      );
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Moderation action failed'));
    } finally {
      setBusy(false);
    }
  };

  const handleUnsuspend = async () => {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await adminApi.unsuspendUser(userId);
      await load();
      setSuccess('User unsuspended.');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to unsuspend user'));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <p>Loading user…</p>;
  if (!user) return <p className="error-text">{error ?? 'User not found'}</p>;

  const canModerate = user.accountStatus !== 'DELETED';

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>{user.name}</h1>
          <p className="dashboard-subtitle">
            <Link to="/admin/users">← Back to users</Link>
          </p>
        </div>
        <span className={`status-pill status-${user.accountStatus.toLowerCase()}`}>
          {user.accountStatus}
        </span>
      </div>

      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      <section className="admin-detail-card">
        <dl className="admin-dl">
          <div>
            <dt>Email</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{user.role}</dd>
          </div>
          <div>
            <dt>Warnings</dt>
            <dd>{user.warningCount}</dd>
          </div>
          <div>
            <dt>Member since</dt>
            <dd>{new Date(user.createdAt).toLocaleDateString()}</dd>
          </div>
          {user.deletedAt && (
            <div>
              <dt>Deleted at</dt>
              <dd>{new Date(user.deletedAt).toLocaleString()}</dd>
            </div>
          )}
        </dl>
      </section>

      {canModerate && (
        <section className="admin-detail-card">
          <h2>Actions</h2>
          <div className="admin-resolve-actions">
            <button type="button" className="btn-soft btn-sm" disabled={busy} onClick={() => setAction('warn')}>
              Warn
            </button>
            {user.accountStatus !== 'SUSPENDED' ? (
              <button
                type="button"
                className="btn-danger-outline btn-sm"
                disabled={busy}
                onClick={() => setAction('suspend')}
              >
                Suspend
              </button>
            ) : (
              <button type="button" className="btn-secondary btn-sm" disabled={busy} onClick={handleUnsuspend}>
                Unsuspend
              </button>
            )}
            <button
              type="button"
              className="btn-danger-outline btn-sm"
              disabled={busy}
              onClick={() => setAction('delete')}
            >
              Soft delete
            </button>
          </div>

          {action && (
            <form className="admin-resolve-form" onSubmit={runAction}>
              <label>
                Reason for {action}
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  rows={3}
                  required
                  autoFocus
                />
              </label>
              <div className="admin-resolve-actions">
                <button type="submit" className="btn-primary" disabled={busy}>
                  {busy ? 'Working…' : 'Confirm'}
                </button>
                <button
                  type="button"
                  className="btn-ghost"
                  disabled={busy}
                  onClick={() => {
                    setAction(null);
                    setReason('');
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </section>
      )}

      <section className="admin-detail-card">
        <h2>Warning history</h2>
        {user.warnings.length === 0 ? (
          <p className="empty-state">No warnings yet.</p>
        ) : (
          <ul className="admin-warning-list">
            {user.warnings.map((warning) => (
              <li key={warning.id}>
                <div className="admin-list-title">{warning.reason}</div>
                <div className="admin-list-meta">
                  by {warning.issuedByAdminName} · {new Date(warning.createdAt).toLocaleString()}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
