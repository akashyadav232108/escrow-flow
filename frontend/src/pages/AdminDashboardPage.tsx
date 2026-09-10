import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import type { AdminDashboardStats } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

function formatMoney(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(value);
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.getDashboard();
        if (!cancelled) setStats(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load dashboard'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>Admin dashboard</h1>
          <p className="dashboard-subtitle">Platform overview and quick links.</p>
        </div>
      </div>

      {loading && <p>Loading stats…</p>}
      {error && <p className="error-text">{error}</p>}

      {stats && (
        <>
          <div className="admin-stat-grid">
            <div className="admin-stat-card">
              <span className="admin-stat-label">Total users</span>
              <span className="admin-stat-value">{stats.totalUsers}</span>
              <span className="admin-stat-meta">
                {stats.clients} clients · {stats.freelancers} freelancers · {stats.both} both ·{' '}
                {stats.admins} admins
              </span>
            </div>
            <div className="admin-stat-card">
              <span className="admin-stat-label">Moderation</span>
              <span className="admin-stat-value">{stats.warnedUsers + stats.suspendedUsers}</span>
              <span className="admin-stat-meta">
                {stats.warnedUsers} warned · {stats.suspendedUsers} suspended
              </span>
            </div>
            <div className="admin-stat-card highlight">
              <span className="admin-stat-label">Open disputes</span>
              <span className="admin-stat-value">{stats.disputedMilestones}</span>
              <Link to="/admin/disputes" className="admin-stat-link">
                Review disputes →
              </Link>
            </div>
            <div className="admin-stat-card">
              <span className="admin-stat-label">Escrow held</span>
              <span className="admin-stat-value">{formatMoney(stats.totalEscrowHeld)}</span>
              <span className="admin-stat-meta">Currently locked in milestones</span>
            </div>
            <div className="admin-stat-card">
              <span className="admin-stat-label">Projects</span>
              <span className="admin-stat-value">{stats.openProjects + stats.inProgressProjects}</span>
              <span className="admin-stat-meta">
                {stats.openProjects} open · {stats.inProgressProjects} in progress ·{' '}
                {stats.completedProjects} completed
              </span>
            </div>
          </div>

          <section className="dashboard-section">
            <h2>Quick actions</h2>
            <div className="admin-quick-links">
              <Link to="/admin/disputes" className="btn-primary">
                Open disputes
              </Link>
              <Link to="/admin/users" className="btn-secondary">
                Manage users
              </Link>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
