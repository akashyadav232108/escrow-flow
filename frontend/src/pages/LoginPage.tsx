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
  const [successMessage, setSuccessMessage] = useState<string | null>(
    actionMessage && !actionMessage.includes('sign up') && !actionMessage.includes('log in to') 
      ? actionMessage 
      : null
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    try {
      const result = await dispatch(login({ email, password })).unwrap();
      const dest = isAdminRole(result.user.role)
        ? from.startsWith('/admin')
          ? from
          : '/admin'
        : from;
      navigate(dest, { replace: true });
    } catch (err) {
      const errorMessage = extractApiErrorMessage(err, 'Login failed');
      
      // Check if error is email not verified
      if (errorMessage.includes('verify your email') || errorMessage.includes('EMAIL_NOT_VERIFIED')) {
        setError('Please verify your email before logging in.');
        // Store email for potential verification redirect
        if (email) {
          sessionStorage.setItem('unverified-email', email);
        }
      } else {
        setError(errorMessage);
      }
    }
  };

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
          <div style={{ textAlign: 'right', marginTop: '-0.5rem' }}>
            <Link 
              to="/forgot-password" 
              style={{ 
                fontSize: '0.85rem', 
                color: 'var(--color-primary)', 
                textDecoration: 'none' 
              }}
            >
              Forgot password?
            </Link>
          </div>
          {successMessage && <p className="success-text">{successMessage}</p>}
          {error && (
            <div>
              <p className="error-text">{error}</p>
              {error.includes('verify your email') && (
                <div style={{ marginTop: '0.5rem', textAlign: 'center' }}>
                  <Link
                    to="/verify-email"
                    state={{ email: email || sessionStorage.getItem('unverified-email') }}
                    style={{
                      fontSize: '0.85rem',
                      color: 'var(--color-primary)',
                      textDecoration: 'underline',
                    }}
                  >
                    Go to verification page
                  </Link>
                </div>
              )}
            </div>
          )}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Logging in…' : 'Log in'}
          </button>
        </form>
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
