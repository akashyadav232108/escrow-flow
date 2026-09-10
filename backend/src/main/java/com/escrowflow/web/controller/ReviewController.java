package com.escrowflow.web.controller;

import com.escrowflow.service.ReviewService;
import com.escrowflow.web.dto.CreateReviewRequest;
import com.escrowflow.web.dto.RatingSummaryResponse;
import com.escrowflow.web.dto.ReviewListResponse;
import com.escrowflow.web.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/projects/{projectId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(projectId, request);
    }

    @GetMapping("/users/{userId}/reviews")
    public ReviewListResponse listForFreelancer(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listForFreelancer(userId, page, size);
    }

    @GetMapping("/users/{userId}/rating-summary")
    public RatingSummaryResponse ratingSummary(@PathVariable Long userId) {
        return reviewService.ratingSummary(userId);
    }
}
