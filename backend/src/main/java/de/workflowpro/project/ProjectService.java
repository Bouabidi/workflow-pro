package de.workflowpro.project;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    static final int MAX_PAGE_SIZE = 100;
    static final Set<String> SORTABLE = Set.of("id", "name", "status", "startDate", "dueDate", "createdAt", "updatedAt");

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        apply(project, request);
        return ProjectResponse.from(repository.save(project));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported sort property: " + order.getProperty());
            }
        }
        Page<ProjectResponse> page = repository.findAll(pageable).map(ProjectResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(find(id));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        apply(project, request);
        return ProjectResponse.from(repository.saveAndFlush(project));
    }

    public ProjectResponse archive(Long id) {
        Project project = find(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        return ProjectResponse.from(repository.saveAndFlush(project));
    }

    private Project find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private void apply(Project project, ProjectRequest r) {
        project.setName(r.name().trim());
        project.setDescription(r.description());
        project.setStatus(r.status() == null ? ProjectStatus.PLANNED : r.status());
        project.setStartDate(r.startDate());
        project.setDueDate(r.dueDate());
    }
}