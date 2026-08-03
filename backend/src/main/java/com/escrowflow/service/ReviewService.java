package com.escrowflow.service;

import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.Review;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.ReviewRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Freelancer reviews (v1 create path).
 * Eligibility: only the project client; at least one milestone APPROVED; one review per project.
 * List / rating-summary APIs come in a later step.
 */
@Service
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;

    public ReviewService(ReviewRepository reviewRepository, ProjectRepository projectRepository) {
        this.reviewRepository = reviewRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Creates a review for the project. Caller must be the project client.
     * Rating must be 1–5; comment is optional.
     */
    @Transactional
    public Review create(Long projectId, Long reviewerId, int rating, String comment) {
        rejectAdminReviewAccess();
        validateRating(rating);

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertEligibleToReview(project, reviewerId);

        Instant now = Instant.now();
        Review review = reviewRepository.save(Review.builder()
                .project(project)
                .reviewer(project.getClient())
                .freelancer(project.getFreelancer())
                .rating(rating)
                .comment(blankToNull(comment))
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "Review created: id={} projectId={} freelancerId={} rating={}",
                review.getId(),
                projectId,
                project.getFreelancer().getId(),
                rating);
        return review;
    }

    /**
     * Option A: client may review when a freelancer is assigned and at least one milestone is APPROVED.
     */
    void assertEligibleToReview(Project project, Long reviewerId) {
        User client = project.getClient();
        if (client == null || !client.getId().equals(reviewerId)) {
            throw new ForbiddenException("Only the project client can leave a review");
        }

        User freelancer = project.getFreelancer();
        if (freelancer == null) {
            throw new IllegalStateException("Project has no assigned freelancer to review");
        }

        if (reviewRepository.existsByProjectId(project.getId())) {
            throw new IllegalStateException("A review already exists for this project");
        }

        boolean hasApprovedMilestone = project.getMilestones().stream()
                .map(Milestone::getStatus)
                .anyMatch(status -> status == MilestoneStatus.APPROVED);
        if (!hasApprovedMilestone) {
            throw new IllegalStateException(
                    "At least one milestone must be APPROVED before leaving a review");
        }
    }

    private void rejectAdminReviewAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins cannot leave reviews");
        }
    }

    private static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    private static String blankToNull(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
