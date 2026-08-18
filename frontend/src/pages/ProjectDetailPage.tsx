import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import FreelancerRating from '../components/FreelancerRating';
import MilestoneList from '../components/MilestoneList';
import ProjectAgreementSection from '../components/ProjectAgreementSection';
import ProjectApplicationsSection from '../components/ProjectApplicationsSection';
import ProjectExitSection from '../components/ProjectExitSection';
import ProjectReviewSection from '../components/ProjectReviewSection';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchProjectById } from '../store/slices/projectsSlice';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const { selectedProject, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);
  const [ratingRefreshKey, setRatingRefreshKey] = useState(0);
  const [agreementReady, setAgreementReady] = useState(true);

  useEffect(() => {
    if (id) {
      dispatch(fetchProjectById(Number(id)));
    }
  }, [dispatch, id]);

  useEffect(() => {
    setAgreementReady(true);
  }, [selectedProject?.id]);

  if (loading || !selectedProject) {
    return <p>{error ?? 'Loading project…'}</p>;
  }

  const isClient = user?.id === selectedProject.client?.id;
  const isFreelancer = user?.id === selectedProject.freelancer?.id;
  const canApplyAsFreelancer =
    !isClient &&
    (user?.role === 'FREELANCER' || user?.role === 'BOTH');

  const refreshProject = () => {
    void dispatch(fetchProjectById(selectedProject.id));
  };

  const milestonesBlocked =
    selectedProject.status === 'EXIT_DISPUTED' || !agreementReady;

  return (
    <div className="project-detail-page">
      <div className="project-detail-header">
        <div>
          <h1>{selectedProject.title}</h1>
          <span className={`status-badge status-${selectedProject.status.toLowerCase()}`}>
            {selectedProject.status}
          </span>
        </div>
      </div>
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
      <ProjectApplicationsSection
        projectId={selectedProject.id}
        projectStatus={selectedProject.status}
        hasFreelancer={Boolean(selectedProject.freelancer)}
        isClient={isClient}
        canApplyAsFreelancer={canApplyAsFreelancer}
        onHired={refreshProject}
      />
      {selectedProject.freelancer && (
        <ProjectAgreementSection
          projectId={selectedProject.id}
          isClient={isClient}
          isAssignedFreelancer={isFreelancer}
          onReadyChange={setAgreementReady}
        />
      )}
      <ProjectExitSection
        projectId={selectedProject.id}
        projectStatus={selectedProject.status}
        isClient={isClient}
        isAssignedFreelancer={isFreelancer}
        onChanged={refreshProject}
      />
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
      {!agreementReady && selectedProject.freelancer && (isClient || isFreelancer) && (
        <p className="muted-text agreement-gate-note">
          Milestone actions are paused until both parties accept the project agreement.
        </p>
      )}
      <MilestoneList
        milestones={selectedProject.milestones ?? []}
        projectId={selectedProject.id}
        isClient={isClient}
        isFreelancer={isFreelancer}
        actionsDisabled={milestonesBlocked}
      />
    </div>
  );
}
