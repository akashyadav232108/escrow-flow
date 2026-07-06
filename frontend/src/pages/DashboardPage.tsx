import { useEffect } from 'react';
import ProjectCard from '../components/ProjectCard';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchProjects } from '../store/slices/projectsSlice';

export default function DashboardPage() {
  const dispatch = useAppDispatch();
  const { projects, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);

  useEffect(() => {
    dispatch(fetchProjects());
  }, [dispatch]);

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <h1>{user?.role === 'FREELANCER' ? 'Open projects' : 'My projects'}</h1>
      </div>
      {loading && <p>Loading projects…</p>}
      {error && <p className="error-text">{error}</p>}
      <div className="project-grid">
        {projects.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
        {!loading && projects.length === 0 && <p>No projects yet.</p>}
      </div>
    </div>
  );
}
