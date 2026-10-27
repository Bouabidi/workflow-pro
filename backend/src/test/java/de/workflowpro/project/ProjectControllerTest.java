package de.workflowpro.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ProjectControllerTest {

    private final ProjectService service = mock(ProjectService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProjectController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(new SortHandlerMethodArgumentResolver()))
                .setValidator(validator)
                .build();
    }

    private ProjectResponse sample() {
        return new ProjectResponse(1L, "Alpha", null, ProjectStatus.PLANNED, null, null, Instant.now(), Instant.now());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(sample());
        mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alpha\",\"startDate\":\"2026-01-01\",\"dueDate\":\"2026-02-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/projects/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void blankNameIs400WithFieldError() throws Exception {
        mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void dueBeforeStartIs400() throws Exception {
        mvc.perform(put("/api/v1/projects/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"startDate\":\"2026-03-01\",\"dueDate\":\"2026-02-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateRangeValid").exists());
    }

    @Test
    void invalidStatusEnumIs400() throws Exception {
        mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingProjectIs404() throws Exception {
        when(service.get(7L)).thenThrow(new ProjectNotFoundException(7L));
        mvc.perform(get("/api/v1/projects/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project 7 not found"));
    }

    @Test
    void archiveMissingIs404AndArchiveOk() throws Exception {
        when(service.archive(7L)).thenThrow(new ProjectNotFoundException(7L));
        mvc.perform(patch("/api/v1/projects/7/archive")).andExpect(status().isNotFound());
        when(service.archive(1L)).thenReturn(sample());
        mvc.perform(patch("/api/v1/projects/1/archive")).andExpect(status().isOk());
    }

    @Test
    void listPassesPagingAndSort() throws Exception {
        when(service.list(any())).thenReturn(new PageResponse<>(List.of(sample()), 2, 5, 11, 3));
        mvc.perform(get("/api/v1/projects?page=2&size=5&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(3));
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(c.capture());
        org.assertj.core.api.Assertions.assertThat(c.getValue().getPageNumber()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(c.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void unknownSortIs400() throws Exception {
        when(service.list(any())).thenThrow(new IllegalArgumentException("Unsupported sort property: x"));
        mvc.perform(get("/api/v1/projects?sort=x")).andExpect(status().isBadRequest());
    }
}