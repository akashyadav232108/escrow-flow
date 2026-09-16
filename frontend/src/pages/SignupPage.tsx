import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import BackButton from '../components/BackButton';
import PasswordInput from '../components/PasswordInput';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { signup } from '../store/slices/authSlice';
import type { Role } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

interface LocationState {
  from?: { pathname: string };
  action?: string;
  message?: string;
}

export default function SignupPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as LocationState | undefined;
  const loading = useAppSelector((state) => state.auth.loading);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('CLIENT');
  const [error, setError] = useState<string | null>(null);

  const redirectPath = locationState?.from?.pathname || '/';
  const actionMessage = locationState?.message;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      await dispatch(signup({ name, email, password, role })).unwrap();
      navigate(redirectPath, { replace: true });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Signup failed'));
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
        <h1>Create your account</h1>
        <p className="auth-subtitle">
          {actionMessage || 'Milestone-based escrow for freelance work, done right.'}
        </p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Jane Doe" required />
          </label>
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
              placeholder="At least 8 characters"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          <label>
            I am a
            <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
              <option value="CLIENT">Client — hiring for a project</option>
              <option value="FREELANCER">Freelancer — delivering work</option>
              <option value="BOTH">Both</option>
            </select>
          </label>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Creating account…' : 'Sign up'}
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
          Already have an account?{' '}
          <Link to="/login" state={locationState}>
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
