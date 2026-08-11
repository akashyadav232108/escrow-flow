import type { Milestone } from '../types';
import MilestoneActions from './MilestoneActions';

const STATUS_CLASS: Record<Milestone['status'], string> = {
  PENDING: 'status-pending',
  FUNDS_LOCKED: 'status-funds-locked',
  SUBMITTED: 'status-submitted',
  APPROVED: 'status-approved',
  DISPUTED: 'status-disputed',
  REFUNDED: 'status-refunded',
  SETTLED: 'status-settled',
};

interface MilestoneListProps {
  milestones: Milestone[];
  projectId: number;
  isClient: boolean;
  isFreelancer: boolean;
  actionsDisabled?: boolean;
}

export default function MilestoneList({
  milestones,
  projectId,
  isClient,
  isFreelancer,
  actionsDisabled = false,
}: MilestoneListProps) {
  return (
    <ul className="milestone-list">
      {milestones.map((milestone) => (
        <li key={milestone.id} className="milestone-row">
          <div className="milestone-info">
            <span className="milestone-title">{milestone.title}</span>
            <span className={`status-badge ${STATUS_CLASS[milestone.status]}`}>{milestone.status}</span>
            <span className="milestone-amount">₹{milestone.amount}</span>
          </div>
          {!actionsDisabled && (
            <MilestoneActions
              milestone={milestone}
              projectId={projectId}
              isClient={isClient}
              isFreelancer={isFreelancer}
            />
          )}
        </li>
      ))}
    </ul>
  );
}
