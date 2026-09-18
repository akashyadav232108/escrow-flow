import { useEffect } from 'react';
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

interface MilestoneDetailModalProps {
  milestone: Milestone;
  projectId: number;
  isClient: boolean;
  isFreelancer: boolean;
  actionsDisabled?: boolean;
  onClose: () => void;
}

const urlPattern = /(https?:\/\/[^\s]+)/g;
const singleUrlPattern = /^https?:\/\/[^\s]+$/;

function renderSubmittedNote(note: string) {
  return note.split(urlPattern).map((part, index) => {
    if (singleUrlPattern.test(part)) {
      return (
        <a key={`${part}-${index}`} href={part} target="_blank" rel="noreferrer">
          {part}
        </a>
      );
    }
    return <span key={index}>{part}</span>;
  });
}

export default function MilestoneDetailModal({
  milestone,
  projectId,
  isClient,
  isFreelancer,
  actionsDisabled = false,
  onClose,
}: MilestoneDetailModalProps) {
  // Prevent body scroll when modal is open
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = '';
    };
  }, []);

  // Close on Escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content milestone-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{milestone.title}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        
        <div className="modal-body">
          <div className="milestone-detail-info">
            <div className="milestone-detail-row">
              <span className="milestone-detail-label">Status</span>
              <span className={`status-badge ${STATUS_CLASS[milestone.status]}`}>
                {milestone.status}
              </span>
            </div>
            
            <div className="milestone-detail-row">
              <span className="milestone-detail-label">Amount</span>
              <span className="milestone-detail-amount">₹{milestone.amount}</span>
            </div>

            {milestone.description && (
              <div className="milestone-detail-row">
                <span className="milestone-detail-label">Description</span>
                <p className="milestone-detail-description">{milestone.description}</p>
              </div>
            )}

            {milestone.submittedNote && (
              <div className="milestone-detail-row">
                <span className="milestone-detail-label">Submitted Work</span>
                <div className="milestone-detail-submitted">
                  {renderSubmittedNote(milestone.submittedNote)}
                </div>
              </div>
            )}
          </div>

          {!actionsDisabled && (
            <div className="milestone-detail-actions">
              <MilestoneActions
                milestone={milestone}
                projectId={projectId}
                isClient={isClient}
                isFreelancer={isFreelancer}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
