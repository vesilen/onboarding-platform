package be.onboarding.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ApplicationService service = new ApplicationService(() -> FIXED_ID);

    @Test
    void submitAssignsIdAndMarksApplicationAsReceived() {
        var response = service.submit(new ApplicationRequest("Ada", "Lovelace", "ada@example.com"));

        assertThat(response.applicationId()).isEqualTo(FIXED_ID);
        assertThat(response.status()).isEqualTo(ApplicationStatus.RECEIVED);
    }
}
