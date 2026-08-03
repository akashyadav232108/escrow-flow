package com.escrowflow.web.controller;

import com.escrowflow.service.ProjectApplicationService;
import com.escrowflow.web.dto.ApplicationResponse;
import com.escrowflow.web.dto.ApplyToProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    public ProjectApplicationController(ProjectApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/projects/{projectId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(
            @PathVariable Long projectId,
            @Valid @RequestBody(required = false) ApplyToProjectRequest request) {
        return applicationService.apply(projectId, request != null ? request : new ApplyToProjectRequest(null));
    }

    @GetMapping("/projects/{projectId}/applications")
    public List<ApplicationResponse> listForProject(@PathVariable Long projectId) {
        return applicationService.listForProject(projectId);
    }

    @GetMapping("/applications/mine")
    public List<ApplicationResponse> listMine() {
        return applicationService.listMine();
    }

    @PostMapping("/applications/{applicationId}/accept")
    public ApplicationResponse accept(@PathVariable Long applicationId) {
        return applicationService.accept(applicationId);
    }

    @PostMapping("/applications/{applicationId}/decline")
    public ApplicationResponse decline(@PathVariable Long applicationId) {
        return applicationService.decline(applicationId);
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    public ApplicationResponse withdraw(@PathVariable Long applicationId) {
        return applicationService.withdraw(applicationId);
    }
}
