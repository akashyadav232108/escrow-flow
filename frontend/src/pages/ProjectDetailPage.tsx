import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import FreelancerRating from '../components/FreelancerRating';
import MilestoneList from '../components/MilestoneList';
import ProjectReviewSection from '../components/ProjectReviewSection';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { acceptProject, fetchProjectById } from '../store/slices/projectsSlice';
import { extractApiErrorMessage } from '../utils/errors';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const { selectedProject, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);
  const [accepting, setAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);
  const [ratingRefreshKey, setRatingRefreshKey] = useState(0);

  useEffect(() => {
    if (id) {
      dispatch(fetchProjectById(Number(id)));
    }
  }, [dispatch, id]);

  if (loading || !selectedProject) {
    return <p>{error ?? 'Loading project…'}</p>;
  }

  const isClient = user?.id === selectedProject.client?.id;
  const isFreelancer = user?.id === selectedProject.freelancer?.id;
  const canAccept =
    selectedProject.status === 'OPEN' &&
    !selectedProject.freelancer &&
    (user?.role === 'FREELANCER' || user?.role === 'BOTH');

  const handleAccept = async () => {
    setAccepting(true);
    setAcceptError(null);
    try {
      await dispatch(acceptProject(selectedProject.id)).unwrap();
    } catch (err) {
      setAcceptError(extractApiErrorMessage(err, 'Failed to accept project'));
    } finally {
      setAccepting(false);
    }
  };

  return (
    <div className="project-detail-page">
      <div className="project-detail-header">
        <div>
          <h1>{selectedProject.title}</h1>
          <span className={`status-badge status-${selectedProject.status.toLowerCase()}`}>
            {selectedProject.status}
          </span>
        </div>
        {canAccept && (
          <button type="button" className="btn-primary" disabled={accepting} onClick={handleAccept}>
            {accepting ? 'Accepting…' : 'Accept project'}
          </button>
        )}
      </div>
      {acceptError && <p className="error-text">{acceptError}</p>}
      {selectedProject.description && <p className="project-description">{selectedProject.description}</p>}
      <div className="project-meta">
        <span>Client: {selectedProject.client?.name}</span>
        <span className="project-meta-freelancer">
          Freelancer: {selectedProject.freelancer?.name ?? 'Unassigned'}
          {selectedProject.freelancer && (
            <FreelancerRating
              freelancerId={selectedProject.freelancer.id}
              refreshKey={ratingRefreshKey}
            />
          )}
        </span>
      </div>
      {selectedProject.freelancer && (
        <ProjectReviewSection
          projectId={selectedProject.id}
          freelancerId={selectedProject.freelancer.id}
          freelancerName={selectedProject.freelancer.name}
          milestones={selectedProject.milestones ?? []}
          isClient={isClient}
          onReviewCreated={() => setRatingRefreshKey((k) => k + 1)}
        />
      )}
      <MilestoneList
        milestones={selectedProject.milestones ?? []}
        projectId={selectedProject.id}
        isClient={isClient}
        isFreelancer={isFreelancer}
      />
    </div>
  );
}
