package de.workflowpro.project;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate dueDate) {

    @AssertTrue(message = "dueDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || dueDate == null || !dueDate.isBefore(startDate);
    }
}