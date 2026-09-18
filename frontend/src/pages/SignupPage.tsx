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
      const signupEmail = await dispatch(signup({ name, email, password, role })).unwrap();
      // Redirect to verify email page instead of logging in
      navigate('/verify-email', { state: { email: signupEmail }, replace: true });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Signup failed'));
    }
  };

  return (
    <div className="auth-page">
      {/* Info Panel - Desktop Only */}
      <div className="auth-info-panel">
        <div className="auth-info-content">
          <h1>Start your journey with Escrow Flow</h1>
          <p>Join thousands of clients and freelancers who trust us with their projects</p>
          <div className="auth-info-features">
            <div className="auth-feature">
              <div className="auth-feature-icon">💼</div>
              <div className="auth-feature-text">
                <h3>For Clients</h3>
                <p>Post projects, approve work, and pay with confidence</p>
              </div>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">💻</div>
              <div className="auth-feature-text">
                <h3>For Freelancers</h3>
                <p>Apply to projects and get paid securely for your work</p>
              </div>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">✨</div>
              <div className="auth-feature-text">
                <h3>Free to Start</h3>
                <p>Get $10,000 in your wallet to begin immediately</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Form Section */}
      <div className="auth-form-section">
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
        <p className="auth-footer">
          Already have an account?{' '}
          <Link to="/login" state={locationState}>
            Log in
          </Link>
        </p>
        </div>
      </div>
    </div>
  );
}
