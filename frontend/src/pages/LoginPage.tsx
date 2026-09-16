import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import BackButton from '../components/BackButton';
import PasswordInput from '../components/PasswordInput';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { login } from '../store/slices/authSlice';
import { extractApiErrorMessage } from '../utils/errors';
import { isAdminRole } from '../utils/roles';

interface LocationState {
  from?: { pathname: string };
  action?: string;
  message?: string;
}

export default function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as LocationState | undefined;
  const loading = useAppSelector((state) => state.auth.loading);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const from = locationState?.from?.pathname ?? '/';
  const actionMessage = locationState?.message;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      const result = await dispatch(login({ email, password })).unwrap();
      const dest = isAdminRole(result.user.role)
        ? from.startsWith('/admin')
          ? from
          : '/admin'
        : from;
      navigate(dest, { replace: true });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Login failed'));
    }
  };

  const handleGoBack = () => {
    // Use browser back navigation for better UX
    navigate(-1);
  };

  // Only show continue browsing if user came from a protected action
  const showContinueBrowsing = locationState?.from && locationState?.action;

  return (
    <div className="auth-page">
      <BackButton className="auth-back-button" />
      <div className="auth-brand">
        <span className="auth-brand-mark">E</span>
        Escrow Flow
      </div>
      <div className="auth-card">
        <h1>Welcome back</h1>
        <p className="auth-subtitle">{actionMessage || 'Log in to manage your projects and wallet.'}</p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
            />
          </label>
          <label>
            Password
            <PasswordInput
              value={password}
              onChange={setPassword}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />
          </label>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Logging in…' : 'Log in'}
          </button>
        </form>
        {showContinueBrowsing && (
          <button
            type="button"
            className="btn-secondary"
            onClick={handleGoBack}
            style={{ width: '100%', marginTop: '0.5rem' }}
          >
            Continue Browsing
          </button>
        )}
        <p className="auth-footer">
          No account?{' '}
          <Link to="/signup" state={locationState}>
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
