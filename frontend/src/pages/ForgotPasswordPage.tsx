import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import BackButton from '../components/BackButton';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { forgotPassword } from '../store/slices/authSlice';
import { extractApiErrorMessage } from '../utils/errors';

export default function ForgotPasswordPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const loading = useAppSelector((state) => state.auth.loading);
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    try {
      const resultEmail = await dispatch(forgotPassword({ email })).unwrap();
      navigate('/reset-password', { state: { email: resultEmail }, replace: true });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to send reset code'));
    }
  };

  return (
    <div className="auth-page">
      {/* Info Panel - Desktop Only */}
      <div className="auth-info-panel">
        <div className="auth-info-content">
          <h1>Reset your password</h1>
          <p>We'll send you a code to reset your password securely</p>
          <div className="auth-info-features">
            <div className="auth-feature">
              <div className="auth-feature-icon">🔐</div>
              <div className="auth-feature-text">
                <h3>Secure Process</h3>
                <p>Email-verified OTP code for password reset</p>
              </div>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">⚡</div>
              <div className="auth-feature-text">
                <h3>Quick Reset</h3>
                <p>Get back to your account in minutes</p>
              </div>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">🛡️</div>
              <div className="auth-feature-text">
                <h3>Protected Account</h3>
                <p>Your data stays safe throughout</p>
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
        <h1>Forgot password?</h1>
        <p className="auth-subtitle">
          No worries! Enter your email address and we'll send you a code to reset your password.
        </p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              autoComplete="email"
            />
          </label>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Sending…' : 'Send Reset Code'}
          </button>
        </form>
        <p className="auth-footer">
          Remember your password?{' '}
          <Link to="/login">Log in</Link>
        </p>
        </div>
      </div>
    </div>
  );
}
