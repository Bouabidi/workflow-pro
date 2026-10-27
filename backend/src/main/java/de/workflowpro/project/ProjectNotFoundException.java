package de.workflowpro.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project " + id + " not found");
    }
}