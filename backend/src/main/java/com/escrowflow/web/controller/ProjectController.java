package com.escrowflow.web.controller;

import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.service.ProjectService;
import com.escrowflow.web.dto.CreateProjectRequest;
import com.escrowflow.web.dto.ProjectDetailResponse;
import com.escrowflow.web.dto.ProjectSummaryResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectSummaryResponse> list(@RequestParam(required = false) ProjectStatus status) {
        return projectService.listForUser(status);
    }

    @GetMapping("/{id}")
    public ProjectDetailResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PostMapping("/{id}/accept")
    public ProjectDetailResponse accept(@PathVariable Long id) {
        return projectService.accept(id);
    }
}
