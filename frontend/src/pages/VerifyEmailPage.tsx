import { useState, type FormEvent, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import BackButton from '../components/BackButton';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { verifyEmail, resendOtp } from '../store/slices/authSlice';
import { extractApiErrorMessage } from '../utils/errors';

export default function VerifyEmailPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const loading = useAppSelector((state) => state.auth.loading);
  
  const emailFromState = (location.state as { email?: string })?.email || '';
  const [email] = useState(emailFromState);
  const [otp, setOtp] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [resendLoading, setResendLoading] = useState(false);

  useEffect(() => {
    if (!email) {
      navigate('/signup');
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

    try {
      await dispatch(verifyEmail({ email, otp })).unwrap();
      navigate('/', { replace: true });
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Verification failed'));
    }
  };

  const handleResendOtp = async () => {
    setResendLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await dispatch(resendOtp({ email })).unwrap();
      setSuccess('A new code has been sent to your email');
      setOtp(''); // Clear input
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
        <h1>Verify your email</h1>
        <p className="auth-subtitle">
          We've sent a 6-digit code to <strong>{email}</strong>. Enter it below to verify your account.
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
          {error && <p className="error-text">{error}</p>}
          {success && <p className="success-text">{success}</p>}
          <button type="submit" className="btn-primary" disabled={loading || otp.length !== 6}>
            {loading ? 'Verifying…' : 'Verify Email'}
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
