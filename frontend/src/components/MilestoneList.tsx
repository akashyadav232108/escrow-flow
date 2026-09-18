import { useState } from 'react';
import type { Milestone } from '../types';
import MilestoneDetailModal from './MilestoneDetailModal';

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
  const [selectedMilestone, setSelectedMilestone] = useState<Milestone | null>(null);

  return (
    <>
      <ul className="milestone-list">
        {milestones.map((milestone) => (
          <li 
            key={milestone.id} 
            className="milestone-row milestone-clickable"
            onClick={() => setSelectedMilestone(milestone)}
          >
            <div className="milestone-main">
              <div className="milestone-info">
                <span className="milestone-title">{milestone.title}</span>
                <span className={`status-badge ${STATUS_CLASS[milestone.status]}`}>{milestone.status}</span>
                <span className="milestone-amount">₹{milestone.amount}</span>
              </div>
              {milestone.description && (
                <p className="milestone-description-preview">{milestone.description}</p>
              )}
            </div>
            <div className="milestone-expand-hint">
              <span>View Details →</span>
            </div>
          </li>
        ))}
      </ul>

      {selectedMilestone && (
        <MilestoneDetailModal
          milestone={selectedMilestone}
          projectId={projectId}
          isClient={isClient}
          isFreelancer={isFreelancer}
          actionsDisabled={actionsDisabled}
          onClose={() => setSelectedMilestone(null)}
        />
      )}
    </>
  );
}
