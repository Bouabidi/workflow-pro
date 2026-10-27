package de.workflowpro.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class ProjectServiceTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final ProjectService service = new ProjectService(repository);

    @Test
    void createDefaultsStatusToPlannedAndTrimsName() {
        when(repository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));
        ProjectResponse r = service.create(new ProjectRequest("  Alpha ", null, null, LocalDate.of(2026, 1, 1), null));
        assertThat(r.name()).isEqualTo("Alpha");
        assertThat(r.status()).isEqualTo(ProjectStatus.PLANNED);
    }

    @Test
    void getMissingThrowsNotFound() {
        when(repository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void updateReplacesFields() {
        Project p = new Project();
        p.setName("Old");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.saveAndFlush(p)).thenReturn(p);
        ProjectResponse r = service.update(1L, new ProjectRequest("New", "d", ProjectStatus.IN_PROGRESS, null, null));
        assertThat(r.name()).isEqualTo("New");
        assertThat(r.status()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    void archiveSetsArchivedStatusAndIsIdempotent() {
        Project p = new Project();
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.saveAndFlush(p)).thenReturn(p);
        assertThat(service.archive(1L).status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(service.archive(1L).status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void archiveMissingThrowsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.archive(9L)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void listRejectsUnknownSortAndOversizedPage() {
        assertThatThrownBy(() -> service.list(PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(PageRequest.of(0, 101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listMapsPage() {
        Project p = new Project();
        p.setName("A");
        PageRequest pr = PageRequest.of(0, 10, Sort.by("name"));
        when(repository.findAll(pr)).thenReturn(new PageImpl<>(java.util.List.of(p), pr, 1));
        PageResponse<ProjectResponse> r = service.list(pr);
        assertThat(r.content()).hasSize(1);
        assertThat(r.totalElements()).isEqualTo(1);
    }
}