import { useEffect, useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { adminApi } from '../api/adminApi';
import PasswordInput from '../components/PasswordInput';
import { useAppSelector } from '../store/hooks';
import type { AdminUser } from '../types';
import { extractApiErrorMessage } from '../utils/errors';
import { isSuperAdminRole } from '../utils/roles';

export default function AdminAdminsPage() {
  const role = useAppSelector((state) => state.auth.user?.role);
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const isSuperAdmin = isSuperAdminRole(role);

  const load = async () => {
    const data = await adminApi.listAdmins();
    setAdmins(data);
  };

  useEffect(() => {
    if (!isSuperAdmin) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.listAdmins();
        if (!cancelled) setAdmins(data);
      } catch (err) {
        if (!cancelled) setError(extractApiErrorMessage(err, 'Failed to load admins'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [isSuperAdmin]);

  if (!isSuperAdmin) {
    return <Navigate to="/admin" replace />;
  }

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      await adminApi.createAdmin({ name: name.trim(), email: email.trim(), password });
      setName('');
      setEmail('');
      setPassword('');
      await load();
      setSuccess('Admin created.');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to create admin'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="admin-page">
      <div className="dashboard-header">
        <div>
          <h1>Admins</h1>
          <p className="dashboard-subtitle">Create and review platform administrators.</p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}
      {success && <p className="success-text">{success}</p>}

      <section className="admin-detail-card" style={{ maxWidth: 420 }}>
        <h2>Create admin</h2>
        <form className="profile-form" onSubmit={handleCreate}>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label>
            Temporary password
            <PasswordInput
              value={password}
              onChange={setPassword}
              minLength={8}
              required
              autoComplete="new-password"
            />
          </label>
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? 'Creating…' : 'Create admin'}
          </button>
        </form>
      </section>

      <section className="dashboard-section">
        <h2>All admins</h2>
        {loading && <p>Loading…</p>}
        <div className="table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Created by</th>
              </tr>
            </thead>
            <tbody>
              {admins.map((admin) => (
                <tr key={admin.id}>
                  <td>{admin.name}</td>
                  <td>{admin.email}</td>
                  <td>{admin.role}</td>
                  <td>{admin.createdByName ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
