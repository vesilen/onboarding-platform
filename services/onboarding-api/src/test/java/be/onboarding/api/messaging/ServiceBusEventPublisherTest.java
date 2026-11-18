package be.onboarding.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import be.onboarding.api.application.ApplicationSubmitted;
import be.onboarding.api.application.EventPublicationException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ServiceBusEventPublisherTest {

    private static final ApplicationSubmitted EVENT = new ApplicationSubmitted(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Ada", "Lovelace", "ada@example.com", Instant.parse("2026-11-18T09:00:00Z"));

    private final ServiceBusSenderClient sender = mock(ServiceBusSenderClient.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ServiceBusEventPublisher publisher = new ServiceBusEventPublisher(sender, jsonMapper);

    @Test
    void sendsTheEventAsJsonWithApplicationIdAsMessageId() {
        publisher.publish(EVENT);

        var captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(captor.capture());
        var message = captor.getValue();

        assertThat(message.getMessageId()).isEqualTo(EVENT.applicationId().toString());
        assertThat(message.getContentType()).isEqualTo("application/json");
        assertThat(message.getSubject()).isEqualTo("ApplicationSubmitted");
        // The body is exactly what onboarding-processor will deserialize.
        assertThat(jsonMapper.readValue(message.getBody().toString(), ApplicationSubmitted.class))
                .isEqualTo(EVENT);
    }

    @Test
    void wrapsSendFailuresInEventPublicationException() {
        doThrow(new RuntimeException("namespace unreachable")).when(sender).sendMessage(any(ServiceBusMessage.class));

        assertThatThrownBy(() -> publisher.publish(EVENT))
                .isInstanceOf(EventPublicationException.class)
                .hasMessageContaining(EVENT.applicationId().toString());
    }
}
