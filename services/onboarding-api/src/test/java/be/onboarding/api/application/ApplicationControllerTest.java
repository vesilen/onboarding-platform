package be.onboarding.api.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Web-layer test: the service is mocked with Mockito, so only HTTP mapping and JSON are tested. */
@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ApplicationController(applicationService))
                .build();
    }

    @Test
    void submitReturnsAcceptedWithApplicationId() throws Exception {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        when(applicationService.submit(any())).thenReturn(new ApplicationResponse(id, ApplicationStatus.RECEIVED));

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.applicationId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(applicationService).submit(new ApplicationRequest("Ada", "Lovelace", "ada@example.com"));
    }

    @Test
    void submitReturnsServiceUnavailableWhenTheEventCannotBePublished() throws Exception {
        when(applicationService.submit(any()))
                .thenThrow(new EventPublicationException("down", new RuntimeException()));

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").exists());
    }
}
