import { useEffect, useState, type FormEvent } from 'react';
import FreelancerRating from '../components/FreelancerRating';
import PasswordInput from '../components/PasswordInput';
import { reviewApi } from '../api/reviewApi';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { changePassword } from '../store/slices/authSlice';
import type { Review } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

export default function ProfilePage() {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.user);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);

  const showsRatings = user?.role === 'FREELANCER' || user?.role === 'BOTH';

  useEffect(() => {
    if (!user || !showsRatings) return;
    let cancelled = false;
    setReviewsLoading(true);
    void reviewApi
      .listForUser(user.id, 0, 10)
      .then((page) => {
        if (!cancelled) setReviews(page.content);
      })
      .catch(() => {
        if (!cancelled) setReviews([]);
      })
      .finally(() => {
        if (!cancelled) setReviewsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [user, showsRatings]);

  if (!user) {
    return null;
  }

  const initial = user.name.trim().charAt(0).toUpperCase();
  const memberSince = user.createdAt
    ? new Date(user.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
    : null;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(false);

    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    setBusy(true);
    try {
      await dispatch(changePassword({ currentPassword, newPassword })).unwrap();
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccess(true);
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to change password'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="profile-page">
      <h1>Profile</h1>

      <section className="profile-card">
        <div className="profile-avatar">{initial}</div>
        <div className="profile-info">
          <div className="profile-name">{user.name}</div>
          <div className="profile-email">{user.email}</div>
          <div className="profile-meta">
            <span className="status-badge status-open">{user.role}</span>
            {memberSince && <span className="profile-member-since">Member since {memberSince}</span>}
          </div>
          {showsRatings && (
            <div className="profile-rating">
              <FreelancerRating freelancerId={user.id} />
            </div>
          )}
        </div>
      </section>

      {showsRatings && (
        <section className="profile-section">
          <h2>Recent reviews</h2>
          {reviewsLoading && <p className="muted-text">Loading…</p>}
          {!reviewsLoading && reviews.length === 0 && (
            <p className="empty-state">No reviews yet.</p>
          )}
          {!reviewsLoading && reviews.length > 0 && (
            <ul className="review-list">
              {reviews.map((review) => (
                <li key={review.id} className="review-list-item">
                  <div className="review-stars-display" aria-label={`${review.rating} out of 5 stars`}>
                    {'★'.repeat(review.rating)}
                    <span className="review-stars-empty">{'★'.repeat(5 - review.rating)}</span>
                  </div>
                  {review.comment && <p className="review-comment">{review.comment}</p>}
                  <p className="muted-text review-meta">
                    {review.reviewerName} · {new Date(review.createdAt).toLocaleDateString()}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <section className="profile-section">
        <h2>Change password</h2>
        <form className="profile-form" onSubmit={handleSubmit}>
          <label>
            Current password
            <PasswordInput
              value={currentPassword}
              onChange={setCurrentPassword}
              autoComplete="current-password"
              required
            />
          </label>
          <label>
            New password
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
            Confirm new password
            <PasswordInput
              value={confirmPassword}
              onChange={setConfirmPassword}
              placeholder="Re-enter new password"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
          {error && <p className="error-text">{error}</p>}
          {success && <p className="success-text">Password updated successfully.</p>}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? 'Updating…' : 'Update password'}
          </button>
        </form>
      </section>
    </div>
  );
}
