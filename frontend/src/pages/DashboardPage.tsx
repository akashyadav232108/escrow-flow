import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import CreateProjectForm from '../components/CreateProjectForm';
import ProjectCard from '../components/ProjectCard';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchProjects } from '../store/slices/projectsSlice';
import { isAdminRole } from '../utils/roles';

export default function DashboardPage() {
  const dispatch = useAppDispatch();
  const { projects, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    if (!isAdminRole(user?.role)) {
      dispatch(fetchProjects(undefined));
    }
  }, [dispatch, user?.role]);

  if (isAdminRole(user?.role)) {
    return <Navigate to="/admin" replace />;
  }

  const isClient = user?.role === 'CLIENT' || user?.role === 'BOTH';
  const isFreelancer = user?.role === 'FREELANCER' || user?.role === 'BOTH';

  const myProjects = projects.filter((project) => project.client?.id === user?.id);
  const myAssignments = projects.filter((project) => project.freelancer?.id === user?.id);
  const openProjects = projects.filter(
    (project) => project.status === 'OPEN' && project.freelancer?.id !== user?.id,
  );

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p className="dashboard-subtitle">
            {isClient && !isFreelancer
              ? "Manage the projects you've created."
              : 'Find work and track your assignments.'}
          </p>
        </div>
        {isClient && (
          <button
            type="button"
            className={showCreateForm ? 'btn-secondary' : 'btn-primary'}
            onClick={() => setShowCreateForm((prev) => !prev)}
          >
            {showCreateForm ? 'Cancel' : '+ New project'}
          </button>
        )}
      </div>

      {showCreateForm && <CreateProjectForm onCreated={() => setShowCreateForm(false)} />}

      {loading && <p>Loading projects…</p>}
      {error && <p className="error-text">{error}</p>}

      {isClient && (
        <section className="dashboard-section">
          <h2>My projects</h2>
          {!loading && myProjects.length === 0 ? (
            <p className="empty-state">You haven't created any projects yet.</p>
          ) : (
            <div className="project-grid">
              {myProjects.map((project) => (
                <ProjectCard key={project.id} project={project} />
              ))}
            </div>
          )}
        </section>
      )}

      {isFreelancer && (
        <>
          <section className="dashboard-section">
            <h2>My assignments</h2>
            {!loading && myAssignments.length === 0 ? (
              <p className="empty-state">No active assignments yet.</p>
            ) : (
              <div className="project-grid">
                {myAssignments.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            )}
          </section>

          <section className="dashboard-section">
            <h2>Open projects</h2>
            {!loading && openProjects.length === 0 ? (
              <p className="empty-state">No open projects available right now.</p>
            ) : (
              <div className="project-grid">
                {openProjects.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
