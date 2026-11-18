package be.onboarding.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationSubmittedHandlerTest {

    private final ApplicationSubmittedHandler handler = new ApplicationSubmittedHandler();

    @Test
    void parsesAValidEvent() {
        var event = handler.handle("""
                {"applicationId":"00000000-0000-0000-0000-000000000001",
                 "firstName":"Ada","lastName":"Lovelace","email":"ada@example.com",
                 "submittedAt":"2026-11-17T10:15:30Z"}
                """);

        assertThat(event.applicationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(event.firstName()).isEqualTo("Ada");
        assertThat(event.submittedAt()).isEqualTo(Instant.parse("2026-11-17T10:15:30Z"));
    }

    @Test
    void ignoresUnknownFields() {
        var event = handler.handle("""
                {"applicationId":"00000000-0000-0000-0000-000000000001","channel":"web"}
                """);

        assertThat(event.applicationId()).isNotNull();
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> handler.handle("not json"))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void rejectsMissingApplicationId() {
        assertThatThrownBy(() -> handler.handle("{\"firstName\":\"Ada\"}"))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("applicationId");
    }

    @Test
    void rejectsEmptyBody() {
        assertThatThrownBy(() -> handler.handle(" "))
                .isInstanceOf(InvalidMessageException.class);
    }
}
