package com.escrowflow.service;

import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.InvalidMilestoneStateException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(MilestoneRepository milestoneRepository) {
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public void submit(Long milestoneId, Long freelancerUserId, String note) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();

        if (project.getFreelancer() == null || !project.getFreelancer().getId().equals(freelancerUserId)) {
            throw new ForbiddenException("Only the assigned freelancer can submit work");
        }

        if (milestone.getStatus() != MilestoneStatus.FUNDS_LOCKED) {
            throw new InvalidMilestoneStateException(
                    "Cannot submit work for milestone in status: " + milestone.getStatus());
        }

        milestone.setSubmittedNote(note);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);

        log.info("Work submitted: milestoneId={} freelancerId={}", milestoneId, freelancerUserId);
    }
}
