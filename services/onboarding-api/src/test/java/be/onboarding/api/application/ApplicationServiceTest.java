package be.onboarding.api.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z");

    private final OnboardingEventPublisher publisher = mock(OnboardingEventPublisher.class);
    private final ApplicationService service =
            new ApplicationService(() -> FIXED_ID, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    private final ApplicationRequest request = new ApplicationRequest("Ada", "Lovelace", "ada@example.com");

    @Test
    void submitAssignsIdAndMarksApplicationAsReceived() {
        var response = service.submit(request);

        assertThat(response.applicationId()).isEqualTo(FIXED_ID);
        assertThat(response.status()).isEqualTo(ApplicationStatus.RECEIVED);
    }

    @Test
    void submitPublishesApplicationSubmittedEvent() {
        service.submit(request);

        verify(publisher).publish(new ApplicationSubmitted(FIXED_ID, "Ada", "Lovelace", "ada@example.com", NOW));
    }

    @Test
    void submitFailsWhenTheEventCannotBePublished() {
        doThrow(new EventPublicationException("down", new RuntimeException()))
                .when(publisher).publish(any());

        assertThatThrownBy(() -> service.submit(request)).isInstanceOf(EventPublicationException.class);
    }
}
