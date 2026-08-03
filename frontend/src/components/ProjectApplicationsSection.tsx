import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { applicationApi } from '../api/applicationApi';
import type { ProjectApplication, ProjectStatus } from '../types';
import { extractApiErrorMessage } from '../utils/errors';
import FreelancerRating from './FreelancerRating';

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

  const handleAccept = async (applicationId: number) => {
    setBusyId(applicationId);
    setError(null);
    try {
      await applicationApi.accept(applicationId);
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
                {openForHire && app.status === 'PENDING' && (
                  <div className="application-actions">
                    <button
                      type="button"
                      className="btn-primary"
                      disabled={busyId === app.id}
                      onClick={() => void handleAccept(app.id)}
                    >
                      {busyId === app.id ? 'Working…' : 'Accept'}
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
              </li>
            ))}
          </ul>
        )}
      </section>
    );
  }

  // Freelancer apply / withdraw
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
