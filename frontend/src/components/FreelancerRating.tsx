import { useEffect, useState } from 'react';
import { reviewApi } from '../api/reviewApi';
import type { RatingSummary } from '../types';

interface FreelancerRatingProps {
  freelancerId: number;
  /** Compact inline for cards; default for detail/profile. */
  compact?: boolean;
  /** Bump to refetch after a new review. */
  refreshKey?: number;
}

function formatAverage(average: number): string {
  return average.toFixed(1);
}

export default function FreelancerRating({
  freelancerId,
  compact = false,
  refreshKey = 0,
}: FreelancerRatingProps) {
  const [summary, setSummary] = useState<RatingSummary | null>(null);

  useEffect(() => {
    let cancelled = false;
    void reviewApi
      .ratingSummary(freelancerId)
      .then((data) => {
        if (!cancelled) setSummary(data);
      })
      .catch(() => {
        if (!cancelled) setSummary(null);
      });
    return () => {
      cancelled = true;
    };
  }, [freelancerId, refreshKey]);

  if (!summary || summary.reviewCount === 0) {
    return compact ? null : <span className="muted-text rating-empty">No reviews yet</span>;
  }

  const label = `★ ${formatAverage(summary.averageRating)} (${summary.reviewCount})`;

  return (
    <span
      className={compact ? 'freelancer-rating freelancer-rating-compact' : 'freelancer-rating'}
      title={`${summary.reviewCount} review${summary.reviewCount === 1 ? '' : 's'}`}
    >
      {label}
    </span>
  );
}
