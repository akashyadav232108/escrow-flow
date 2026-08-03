import { useEffect, useState, type FormEvent } from 'react';
import { reviewApi } from '../api/reviewApi';
import type { Milestone, Review } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

interface ProjectReviewSectionProps {
  projectId: number;
  freelancerId: number;
  freelancerName: string;
  milestones: Milestone[];
  isClient: boolean;
  onReviewCreated?: () => void;
}

function hasApprovedMilestone(milestones: Milestone[]): boolean {
  return milestones.some((m) => m.status === 'APPROVED');
}

export default function ProjectReviewSection({
  projectId,
  freelancerId,
  freelancerName,
  milestones,
  isClient,
  onReviewCreated,
}: ProjectReviewSectionProps) {
  const eligible = isClient && hasApprovedMilestone(milestones);
  const [existing, setExisting] = useState<Review | null>(null);
  const [loading, setLoading] = useState(isClient);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isClient) return;
    let cancelled = false;
    setLoading(true);
    void reviewApi
      .listForUser(freelancerId, 0, 50)
      .then((page) => {
        if (cancelled) return;
        const match = page.content.find((r) => r.projectId === projectId) ?? null;
        setExisting(match);
      })
      .catch(() => {
        if (!cancelled) setExisting(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [freelancerId, projectId, isClient]);

  if (!isClient) {
    return null;
  }

  if (!eligible && !existing && !loading) {
    return null;
  }

  if (loading) {
    return (
      <section className="review-section">
        <h2>Review</h2>
        <p className="muted-text">Loading…</p>
      </section>
    );
  }

  if (existing) {
    return (
      <section className="review-section">
        <h2>Your review</h2>
        <div className="review-existing">
          <div className="review-stars-display" aria-label={`${existing.rating} out of 5 stars`}>
            {'★'.repeat(existing.rating)}
            <span className="review-stars-empty">{'★'.repeat(5 - existing.rating)}</span>
          </div>
          {existing.comment && <p className="review-comment">{existing.comment}</p>}
          <p className="muted-text review-meta">
            For {freelancerName} · {new Date(existing.createdAt).toLocaleDateString()}
          </p>
        </div>
      </section>
    );
  }

  if (!eligible) {
    return null;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const created = await reviewApi.create(projectId, {
        rating,
        comment: comment.trim() || undefined,
      });
      setExisting(created);
      onReviewCreated?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to submit review'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="review-section">
      <h2>Leave a review</h2>
      <p className="muted-text">Rate {freelancerName} for work on this project.</p>
      <form className="review-form" onSubmit={(e) => void handleSubmit(e)}>
        <div className="review-star-picker" role="group" aria-label="Rating">
          {[1, 2, 3, 4, 5].map((value) => (
            <button
              key={value}
              type="button"
              className={`review-star-btn${value <= rating ? ' review-star-btn-active' : ''}`}
              aria-label={`${value} star${value === 1 ? '' : 's'}`}
              aria-pressed={value === rating}
              onClick={() => setRating(value)}
            >
              ★
            </button>
          ))}
        </div>
        <label>
          Comment (optional)
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={5000}
            rows={3}
            placeholder="What went well?"
          />
        </label>
        {error && <p className="error-text">{error}</p>}
        <button type="submit" className="btn-primary" disabled={busy}>
          {busy ? 'Submitting…' : 'Submit review'}
        </button>
      </form>
    </section>
  );
}
