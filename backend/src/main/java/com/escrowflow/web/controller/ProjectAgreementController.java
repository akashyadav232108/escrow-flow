package com.escrowflow.web.controller;

import com.escrowflow.service.ProjectAgreementService;
import com.escrowflow.web.dto.ProjectAgreementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProjectAgreementController {

    private final ProjectAgreementService projectAgreementService;

    public ProjectAgreementController(ProjectAgreementService projectAgreementService) {
        this.projectAgreementService = projectAgreementService;
    }

    @GetMapping("/projects/{projectId}/agreement")
    public ProjectAgreementResponse get(@PathVariable Long projectId) {
        return projectAgreementService.getForProject(projectId);
    }

    @PostMapping("/projects/{projectId}/agreement/accept")
    public ProjectAgreementResponse accept(@PathVariable Long projectId) {
        return projectAgreementService.accept(projectId);
    }
}
