package be.onboarding.api.application;

/** The event could not be handed over to the messaging infrastructure. The caller may retry. */
public class EventPublicationException extends RuntimeException {

    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
