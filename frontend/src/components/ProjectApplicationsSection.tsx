import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { applicationApi } from '../api/applicationApi';
import type { ProjectApplication, ProjectStatus } from '../types';
import { extractApiErrorMessage } from '../utils/errors';
import FreelancerRating from './FreelancerRating';

/** Mirrors backend ProjectAgreementService.DEFAULT_TERMS v1.0 for hire confirmation. */
const HIRE_TERMS_PREVIEW = `Escrow-Flow Project Agreement (v1.0)

1. Scope — Work is defined by this project's milestones and descriptions.
2. Escrow — Client funds milestones before work; funds release on approval or per dispute/exit rules.
3. Delivery — Freelancer submits completed work for each funded milestone in good faith.
4. Disputes — Milestone disputes and project exits are reviewed by platform admins.
5. Settlements — Admin decisions on held escrow (including splits) are final for platform purposes.
6. Evidence — These accepted terms may be referenced by either party during disputes or exits.
7. No automatic penalties — Accepting these terms does not authorize automatic fines; money moves only via escrow, approval, dispute, or exit settlement.

By accepting, you confirm you have read and agree to these terms for this project.`;

interface ProjectApplicationsSectionProps {
  projectId: number;
  projectStatus: ProjectStatus;
  hasFreelancer: boolean;
  isClient: boolean;
  canApplyAsFreelancer: boolean;
  onHired?: () => void;
}

export default function ProjectApplicationsSection({
  projectId,
  projectStatus,
  hasFreelancer,
  isClient,
  canApplyAsFreelancer,
  onHired,
}: ProjectApplicationsSectionProps) {
  const openForHire = projectStatus === 'OPEN' && !hasFreelancer;
  const [applications, setApplications] = useState<ProjectApplication[]>([]);
  const [myApplication, setMyApplication] = useState<ProjectApplication | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [busyId, setBusyId] = useState<number | 'apply' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [acceptingId, setAcceptingId] = useState<number | null>(null);
  const [acceptedTerms, setAcceptedTerms] = useState(false);

  const loadClientList = useCallback(async () => {
    const list = await applicationApi.listForProject(projectId);
    setApplications(list);
  }, [projectId]);

  const loadMyApplication = useCallback(async () => {
    const mine = await applicationApi.listMine();
    setMyApplication(mine.find((a) => a.projectId === projectId) ?? null);
  }, [projectId]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!isClient && !canApplyAsFreelancer) return;
      setLoading(true);
      setError(null);
      try {
        if (isClient) {
          await loadClientList();
        } else if (canApplyAsFreelancer) {
          await loadMyApplication();
        }
      } catch (err) {
        if (!cancelled) {
          setError(extractApiErrorMessage(err, 'Failed to load applications'));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [isClient, canApplyAsFreelancer, loadClientList, loadMyApplication]);

  if (!isClient && !canApplyAsFreelancer) {
    return null;
  }

  const handleApply = async (event: FormEvent) => {
    event.preventDefault();
    setBusyId('apply');
    setError(null);
    try {
      const created = await applicationApi.apply(projectId, {
        message: message.trim() || undefined,
      });
      setMyApplication(created);
      setMessage('');
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to apply'));
    } finally {
      setBusyId(null);
    }
  };

  const handleWithdraw = async (applicationId: number) => {
    setBusyId(applicationId);
    setError(null);
    try {
      const updated = await applicationApi.withdraw(applicationId);
      setMyApplication(updated);
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to withdraw application'));
    } finally {
      setBusyId(null);
    }
  };

  const startAccept = (applicationId: number) => {
    setAcceptingId(applicationId);
    setAcceptedTerms(false);
    setError(null);
  };

  const cancelAccept = () => {
    setAcceptingId(null);
    setAcceptedTerms(false);
  };

  const handleConfirmHire = async (applicationId: number) => {
    if (!acceptedTerms) {
      setError('You must accept the project agreement to hire');
      return;
    }
    setBusyId(applicationId);
    setError(null);
    try {
      await applicationApi.accept(applicationId, true);
      setAcceptingId(null);
      setAcceptedTerms(false);
      await loadClientList();
      onHired?.();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to accept application'));
    } finally {
      setBusyId(null);
    }
  };

  const handleDecline = async (applicationId: number) => {
    setBusyId(applicationId);
    setError(null);
    try {
      await applicationApi.decline(applicationId);
      if (acceptingId === applicationId) {
        cancelAccept();
      }
      await loadClientList();
    } catch (err) {
      setError(extractApiErrorMessage(err, 'Failed to decline application'));
    } finally {
      setBusyId(null);
    }
  };

  if (isClient) {
    return (
      <section className="applications-section">
        <h2>Applications</h2>
        {loading && <p className="muted-text">Loading…</p>}
        {error && <p className="error-text">{error}</p>}
        {!loading && applications.length === 0 && (
          <p className="empty-state">No applications yet.</p>
        )}
        {!loading && applications.length > 0 && (
          <ul className="application-list">
            {applications.map((app) => (
              <li key={app.id} className="application-list-item">
                <div className="application-list-header">
                  <div className="application-applicant">
                    <strong>{app.freelancerName}</strong>
                    <FreelancerRating freelancerId={app.freelancerId} compact />
                  </div>
                  <span className={`status-badge status-${app.status.toLowerCase()}`}>
                    {app.status}
                  </span>
                </div>
                {app.message && <p className="application-message">{app.message}</p>}
                <p className="muted-text application-meta">
                  Applied {new Date(app.createdAt).toLocaleString()}
                </p>
                {openForHire && app.status === 'PENDING' && acceptingId !== app.id && (
                  <div className="application-actions">
                    <button
                      type="button"
                      className="btn-primary"
                      disabled={busyId === app.id}
                      onClick={() => startAccept(app.id)}
                    >
                      Accept
                    </button>
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={busyId === app.id}
                      onClick={() => void handleDecline(app.id)}
                    >
                      Decline
                    </button>
                  </div>
                )}
                {openForHire && app.status === 'PENDING' && acceptingId === app.id && (
                  <div className="hire-agreement-panel">
                    <p className="muted-text">
                      Review and accept the project agreement to hire. The freelancer must also
                      accept before milestone work and escrow continue. No automatic penalties.
                    </p>
                    <pre className="agreement-terms">{HIRE_TERMS_PREVIEW}</pre>
                    <label className="agreement-checkbox">
                      <input
                        type="checkbox"
                        checked={acceptedTerms}
                        onChange={(e) => setAcceptedTerms(e.target.checked)}
                      />
                      I accept these terms and hire this freelancer
                    </label>
                    <div className="application-actions">
                      <button
                        type="button"
                        className="btn-primary"
                        disabled={busyId === app.id || !acceptedTerms}
                        onClick={() => void handleConfirmHire(app.id)}
                      >
                        {busyId === app.id ? 'Hiring…' : 'Confirm hire'}
                      </button>
                      <button
                        type="button"
                        className="btn-secondary"
                        disabled={busyId === app.id}
                        onClick={cancelAccept}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    );
  }

  return (
    <section className="applications-section">
      <h2>Your application</h2>
      {loading && <p className="muted-text">Loading…</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && myApplication && (
        <div className="application-mine">
          <span className={`status-badge status-${myApplication.status.toLowerCase()}`}>
            {myApplication.status}
          </span>
          {myApplication.message && <p className="application-message">{myApplication.message}</p>}
          <p className="muted-text application-meta">
            Applied {new Date(myApplication.createdAt).toLocaleString()}
          </p>
          {openForHire && myApplication.status === 'PENDING' && (
            <button
              type="button"
              className="btn-secondary"
              disabled={busyId === myApplication.id}
              onClick={() => void handleWithdraw(myApplication.id)}
            >
              {busyId === myApplication.id ? 'Withdrawing…' : 'Withdraw application'}
            </button>
          )}
        </div>
      )}

      {!loading && !myApplication && openForHire && (
        <form className="application-form" onSubmit={(e) => void handleApply(e)}>
          <p className="muted-text">Apply to work on this project. The client will choose an applicant.</p>
          <label>
            Message (optional)
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              maxLength={5000}
              rows={3}
              placeholder="Brief note about your fit for this work"
            />
          </label>
          <button type="submit" className="btn-primary" disabled={busyId === 'apply'}>
            {busyId === 'apply' ? 'Applying…' : 'Apply to project'}
          </button>
        </form>
      )}

      {!loading && !myApplication && !openForHire && (
        <p className="muted-text">This project is no longer open for applications.</p>
      )}
    </section>
  );
}
