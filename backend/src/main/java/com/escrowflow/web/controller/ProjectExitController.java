package com.escrowflow.web.controller;

import com.escrowflow.service.ProjectExitService;
import com.escrowflow.web.dto.ProjectExitResponse;
import com.escrowflow.web.dto.RaiseProjectExitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProjectExitController {

    private final ProjectExitService projectExitService;

    public ProjectExitController(ProjectExitService projectExitService) {
        this.projectExitService = projectExitService;
    }

    @PostMapping("/projects/{projectId}/exit")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectExitResponse raise(
            @PathVariable Long projectId,
            @Valid @RequestBody RaiseProjectExitRequest request) {
        return projectExitService.raise(projectId, request);
    }

    @GetMapping("/projects/{projectId}/exit")
    public ProjectExitResponse getOpenForProject(@PathVariable Long projectId) {
        return projectExitService.getOpenForProject(projectId);
    }

    @GetMapping("/project-exits/{exitId}")
    public ProjectExitResponse getById(@PathVariable Long exitId) {
        return projectExitService.getById(exitId);
    }
}
