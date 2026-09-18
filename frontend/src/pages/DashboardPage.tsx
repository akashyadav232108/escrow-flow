import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import CreateProjectForm from '../components/CreateProjectForm';
import ProjectCard from '../components/ProjectCard';
import { useProtectedAction } from '../hooks/useProtectedAction';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchProjects } from '../store/slices/projectsSlice';
import { clearProjectDraft, hasProjectDraft } from '../utils/projectDraft';
import { isAdminRole } from '../utils/roles';

export default function DashboardPage() {
  const dispatch = useAppDispatch();
  const { projects, loading, error } = useAppSelector((state) => state.projects);
  const user = useAppSelector((state) => state.auth.user);
  const { wrapAction, isAuthenticated } = useProtectedAction();
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    // Fetch projects for both guests and authenticated users (except admins)
    if (!user || !isAdminRole(user.role)) {
      dispatch(fetchProjects(undefined));
    }
  }, [dispatch, user]);

  useEffect(() => {
    if (user?.id && hasProjectDraft(user.id)) {
      setShowCreateForm(true);
    }
  }, [user?.id]);

  if (user && isAdminRole(user.role)) {
    return <Navigate to="/admin" replace />;
  }

  const isClient = user?.role === 'CLIENT' || user?.role === 'BOTH';
  const isFreelancer = user?.role === 'FREELANCER' || user?.role === 'BOTH';

  const myProjects = user ? projects.filter((project) => project.client?.id === user.id) : [];
  const myAssignments = user ? projects.filter((project) => project.freelancer?.id === user.id) : [];
  const openProjects = projects.filter(
    (project) => project.status === 'OPEN' && (!user || project.freelancer?.id !== user.id),
  );

  const handleCreateProjectClick = wrapAction(
    () => {
      if (showCreateForm) {
        if (user?.id) clearProjectDraft(user.id);
        setShowCreateForm(false);
        return;
      }
      setShowCreateForm(true);
    },
    { action: 'create-project', message: 'Please sign up or log in to create a project' },
  );

  return (
    <div className="dashboard-page">
      {!isAuthenticated && (
        <div className="guest-banner">
          <h2>Welcome to Escrow Flow 👋</h2>
          <p>
            Discover secure milestone-based escrow payments for freelance projects. Browse open projects below, or join thousands of users who trust us with their work.
          </p>
          <div className="guest-banner-actions">
            <Link to="/signup">Get Started Free</Link>
            <Link to="/about">Learn More</Link>
          </div>
        </div>
      )}

      <div className="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p className="dashboard-subtitle">
            {!isAuthenticated
              ? 'Discover open projects and explore secure escrow payments.'
              : isClient && !isFreelancer
                ? "Manage the projects you've created."
                : 'Find work and track your assignments.'}
          </p>
        </div>
        {(!isAuthenticated || isClient) && (
          <button
            type="button"
            className={showCreateForm ? 'btn-secondary' : 'btn-primary'}
            onClick={handleCreateProjectClick}
          >
            {showCreateForm ? 'Cancel' : '+ New project'}
          </button>
        )}
      </div>

      {showCreateForm && <CreateProjectForm onCreated={() => setShowCreateForm(false)} />}

      {loading && <p>Loading projects…</p>}
      {error && <p className="error-text">{error}</p>}

      {isAuthenticated && isClient && (
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

      {isAuthenticated && isFreelancer && (
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
      )}

      <section className="dashboard-section">
        <h2>{isAuthenticated && isFreelancer ? 'Open projects' : 'Browse Projects'}</h2>
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
    </div>
  );
}
