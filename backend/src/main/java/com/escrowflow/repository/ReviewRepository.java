package com.escrowflow.repository;

import com.escrowflow.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByProjectId(Long projectId);

    Optional<Review> findByProjectId(Long projectId);

    long countByFreelancerId(Long freelancerId);

    @EntityGraph(attributePaths = {"reviewer", "freelancer", "project"})
    Page<Review> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.freelancer.id = :freelancerId")
    Double averageRatingByFreelancerId(@Param("freelancerId") Long freelancerId);
}
