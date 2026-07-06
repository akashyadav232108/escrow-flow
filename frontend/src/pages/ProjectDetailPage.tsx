import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import MilestoneList from '../components/MilestoneList';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchProjectById } from '../store/slices/projectsSlice';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const dispatch = useAppDispatch();
  const { selectedProject, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);

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

  return (
    <div className="project-detail-page">
      <h1>{selectedProject.title}</h1>
      <p>{selectedProject.description}</p>
      <div className="project-meta">
        <span>Status: {selectedProject.status}</span>
        <span>Client: {selectedProject.client?.name}</span>
        <span>Freelancer: {selectedProject.freelancer?.name ?? 'Unassigned'}</span>
      </div>
      <MilestoneList
        milestones={selectedProject.milestones ?? []}
        projectId={selectedProject.id}
        isClient={isClient}
        isFreelancer={isFreelancer}
      />
    </div>
  );
}
