package com.escrowflow.service;

import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.Review;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.ReviewRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.CreateReviewRequest;
import com.escrowflow.web.dto.RatingSummaryResponse;
import com.escrowflow.web.dto.ReviewListResponse;
import com.escrowflow.web.dto.ReviewResponse;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Freelancer reviews (v1 create-only).
 * Eligibility: only the project client; at least one milestone APPROVED; one review per project.
 */
@Service
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse create(Long projectId, CreateReviewRequest request) {
        rejectAdminReviewAccess();
        Long reviewerId = SecurityUtils.getCurrentUserId();
        int rating = request.rating();

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertEligibleToReview(project, reviewerId);

        Instant now = Instant.now();
        Review review = reviewRepository.save(Review.builder()
                .project(project)
                .reviewer(project.getClient())
                .freelancer(project.getFreelancer())
                .rating(rating)
                .comment(blankToNull(request.comment()))
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "Review created: id={} projectId={} freelancerId={} rating={}",
                review.getId(),
                projectId,
                project.getFreelancer().getId(),
                rating);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listForFreelancer(Long freelancerId, int page, int size) {
        requireUserExists(freelancerId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Review> reviewPage =
                reviewRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new ReviewListResponse(
                content,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingSummary(Long freelancerId) {
        requireUserExists(freelancerId);
        long reviewCount = reviewRepository.countByFreelancerId(freelancerId);
        if (reviewCount == 0) {
            return new RatingSummaryResponse(0.0, 0);
        }
        Double average = reviewRepository.averageRatingByFreelancerId(freelancerId);
        double averageRating = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;
        return new RatingSummaryResponse(averageRating, reviewCount);
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

    private void requireUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    private void rejectAdminReviewAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins cannot leave reviews");
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProject().getId(),
                review.getReviewer().getId(),
                review.getReviewer().getName(),
                review.getFreelancer().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    private static String blankToNull(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
