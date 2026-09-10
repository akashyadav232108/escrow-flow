import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import type { AccountStatus, ManagedUser } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

export default function AdminUsersPage() {
  const [status, setStatus] = useState<AccountStatus | 'ALL'>('ALL');
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.listUsers(status === 'ALL' ? undefined : status);
        if (!cancelled) setUsers(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load users'));
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
          <h1>Users</h1>
          <p className="dashboard-subtitle">Warn, suspend, or remove marketplace accounts.</p>
        </div>
        <div className="admin-filter-row">
          {(['ALL', 'ACTIVE', 'WARNED', 'SUSPENDED', 'DELETED'] as const).map((value) => (
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

      {loading && <p>Loading users…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && users.length === 0 && (
        <p className="empty-state">No users in this view.</p>
      )}

      <div className="table-scroll">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Warnings</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  <Link to={`/admin/users/${user.id}`}>{user.name}</Link>
                </td>
                <td>{user.email}</td>
                <td>{user.role}</td>
                <td>
                  <span className={`status-pill status-${user.accountStatus.toLowerCase()}`}>
                    {user.accountStatus}
                  </span>
                </td>
                <td>{user.warningCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
