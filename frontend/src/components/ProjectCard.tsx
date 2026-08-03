import { Link } from 'react-router-dom';
import FreelancerRating from './FreelancerRating';
import type { Project } from '../types';

export default function ProjectCard({ project }: { project: Project }) {
  const milestoneCount = project.milestones?.length ?? 0;

  return (
    <Link to={`/projects/${project.id}`} className="project-card">
      <h3>{project.title}</h3>
      <span className={`status-badge status-${project.status.toLowerCase()}`}>{project.status}</span>
      <p>
        {milestoneCount} milestone{milestoneCount === 1 ? '' : 's'}
      </p>
      {project.freelancer && (
        <p className="project-card-freelancer">
          {project.freelancer.name}{' '}
          <FreelancerRating freelancerId={project.freelancer.id} compact />
        </p>
      )}
    </Link>
  );
}
