import { useState, type FormEvent, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import BackButton from '../components/BackButton';
import PasswordInput from '../components/PasswordInput';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { resetPassword, resendOtp } from '../store/slices/authSlice';
import { extractApiErrorMessage } from '../utils/errors';

export default function ResetPasswordPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const loading = useAppSelector((state) => state.auth.loading);
  
  const emailFromState = (location.state as { email?: string })?.email || '';
  const [email] = useState(emailFromState);
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [resendLoading, setResendLoading] = useState(false);

  useEffect(() => {
    if (!email) {
      navigate('/forgot-password');
    }
  }, [email, navigate]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (otp.length !== 6 || !/^\d+$/.test(otp)) {
      setError('Please enter a valid 6-digit code');
      return;
    }

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      await dispatch(resetPassword({ email, otp, newPassword })).unwrap();
      navigate('/login', { 
        replace: true,
        state: { message: 'Password reset successfully! Please log in.' }
      });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to reset password'));
    }
  };

  const handleResendOtp = async () => {
    setResendLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await dispatch(resendOtp({ email })).unwrap();
      setSuccess('A new code has been sent to your email');
      setOtp('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to resend code'));
    } finally {
      setResendLoading(false);
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
        <h1>Reset your password</h1>
        <p className="auth-subtitle">
          Enter the 6-digit code sent to <strong>{email}</strong> and choose a new password.
        </p>
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Verification Code
            <input
              type="text"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="000000"
              required
              autoComplete="one-time-code"
              inputMode="numeric"
              pattern="[0-9]{6}"
              maxLength={6}
              style={{ letterSpacing: '0.5rem', fontSize: '1.2rem', textAlign: 'center' }}
            />
          </label>
          <label>
            New Password
            <PasswordInput
              value={newPassword}
              onChange={setNewPassword}
              placeholder="At least 8 characters"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          <label>
            Confirm New Password
            <PasswordInput
              value={confirmPassword}
              onChange={setConfirmPassword}
              placeholder="Re-enter your password"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          {error && <p className="error-text">{error}</p>}
          {success && <p className="success-text">{success}</p>}
          <button 
            type="submit" 
            className="btn-primary" 
            disabled={loading || otp.length !== 6 || !newPassword || !confirmPassword}
          >
            {loading ? 'Resetting…' : 'Reset Password'}
          </button>
        </form>
        <div style={{ marginTop: '1rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>
          Didn't receive the code?{' '}
          <button
            type="button"
            onClick={handleResendOtp}
            disabled={resendLoading || loading}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--color-primary)',
              cursor: resendLoading || loading ? 'not-allowed' : 'pointer',
              textDecoration: 'underline',
              padding: 0,
              font: 'inherit',
            }}
          >
            {resendLoading ? 'Sending…' : 'Resend code'}
          </button>
        </div>
      </div>
    </div>
  );
}
