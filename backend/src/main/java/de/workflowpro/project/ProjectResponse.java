package de.workflowpro.project;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(),
                p.getStartDate(), p.getDueDate(), p.getCreatedAt(), p.getUpdatedAt());
    }
}