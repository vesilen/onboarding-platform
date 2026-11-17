package be.onboarding.processor;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a raw queue message into an {@link ApplicationSubmitted} event and processes it.
 *
 * <p>Plain Java without any Azure types, so it can be unit tested without the Functions runtime.
 * For now "processing" means validating the event; the onboarding steps (KYC, credit check, ...)
 * are added in later steps.
 */
public class ApplicationSubmittedHandler {

    private final JsonMapper jsonMapper;

    public ApplicationSubmittedHandler() {
        // Tolerant reader: the producer may add fields without breaking this consumer.
        this(JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
    }

    ApplicationSubmittedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * @throws InvalidMessageException when the body is not valid JSON or a required field is missing
     */
    public ApplicationSubmitted handle(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidMessageException("Empty message body");
        }
        ApplicationSubmitted event;
        try {
            event = jsonMapper.readValue(body, ApplicationSubmitted.class);
        } catch (JacksonException e) {
            throw new InvalidMessageException("Message body is not a valid ApplicationSubmitted event", e);
        }
        if (event == null || event.applicationId() == null) {
            throw new InvalidMessageException("applicationId is missing");
        }
        return event;
    }
}
