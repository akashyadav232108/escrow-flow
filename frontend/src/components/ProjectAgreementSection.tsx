import { useCallback, useEffect, useState } from 'react';
import { agreementApi } from '../api/agreementApi';
import type { ProjectAgreement } from '../types';
import { extractApiErrorMessage } from '../utils/errors';

interface ProjectAgreementSectionProps {
  projectId: number;
  isClient: boolean;
  isAssignedFreelancer: boolean;
  onReadyChange?: (ready: boolean) => void;
}

export default function ProjectAgreementSection({
  projectId,
  isClient,
  isAssignedFreelancer,
  onReadyChange,
}: ProjectAgreementSectionProps) {
  const [agreement, setAgreement] = useState<ProjectAgreement | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [missing, setMissing] = useState(false);
  const [termsExpanded, setTermsExpanded] = useState(true);

  const load = useCallback(async () => {
    if (!isClient && !isAssignedFreelancer) {
      onReadyChange?.(true);
      return;
    }
    setLoading(true);
    setError(null);
    setMissing(false);
    try {
      const data = await agreementApi.get(projectId);
      setAgreement(data);
      setTermsExpanded(!data.fullyAccepted);
      onReadyChange?.(data.fullyAccepted);
    } catch (err) {
      setAgreement(null);
      const message = extractApiErrorMessage(err, 'Failed to load agreement');
      if (message.toLowerCase().includes('not found')) {
        setMissing(true);
        onReadyChange?.(true);
      } else {
        setError(message);
        onReadyChange?.(false);
      }
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- avoid re-fetch loop from parent callback identity
  }, [projectId, isClient, isAssignedFreelancer]);

  useEffect(() => {
    void load();
  }, [load]);

  if (!isClient && !isAssignedFreelancer) {
    return null;
  }

  if (missing) {
    return null;
  }

  const needsMyAccept =
    agreement &&
    ((isClient && !agreement.clientAccepted) ||
      (isAssignedFreelancer && !agreement.freelancerAccepted));

  const handleAccept = async () => {
    setBusy(true);
    setError(null);
    try {
      const updated = await agreementApi.accept(projectId);
      setAgreement(updated);
      setTermsExpanded(!updated.fullyAccepted);
      onReadyChange?.(updated.fullyAccepted);
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to accept agreement'));
    } finally {
      setBusy(false);
    }
  };

  const showFullIntro = !agreement?.fullyAccepted;

  return (
    <section className="applications-section">
      <h2>Project agreement</h2>
      {showFullIntro && (
        <p className="muted-text">
          Both parties must accept these terms before locking funds or submitting work. No automatic
          penalties — terms are evidence for disputes and exits.
        </p>
      )}

      {loading && <p className="muted-text">Loading…</p>}
      {error && <p className="error-text">{error}</p>}

      {agreement && (
        <>
          <div className="agreement-status-row">
            <span className={`status-badge ${agreement.clientAccepted ? 'status-approved' : 'status-pending'}`}>
              Client {agreement.clientAccepted ? 'accepted' : 'pending'}
            </span>
            <span
              className={`status-badge ${agreement.freelancerAccepted ? 'status-approved' : 'status-pending'}`}
            >
              Freelancer {agreement.freelancerAccepted ? 'accepted' : 'pending'}
            </span>
            {agreement.fullyAccepted && (
              <span className="status-badge status-approved">Both accepted</span>
            )}
            {agreement.fullyAccepted && (
              <button
                type="button"
                className="btn-ghost btn-sm agreement-toggle"
                onClick={() => setTermsExpanded((open) => !open)}
              >
                {termsExpanded ? 'Hide terms' : 'View agreement'}
              </button>
            )}
          </div>

          {termsExpanded && (
            <>
              <pre className="agreement-terms">{agreement.termsText}</pre>
              <p className="muted-text application-meta">Version {agreement.termsVersion}</p>
            </>
          )}

          {needsMyAccept && (
            <button type="button" className="btn-primary" disabled={busy} onClick={() => void handleAccept()}>
              {busy ? 'Saving…' : 'I accept these terms'}
            </button>
          )}

          {!agreement.fullyAccepted && !needsMyAccept && (
            <p className="muted-text">Waiting for the other party to accept before milestone work can continue.</p>
          )}
        </>
      )}
    </section>
  );
}
