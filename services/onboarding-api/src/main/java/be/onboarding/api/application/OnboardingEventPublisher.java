package be.onboarding.api.application;

/**
 * Publishes onboarding events to the rest of the platform.
 *
 * <p>The service only knows this interface; whether events go to Service Bus or just to the log is
 * decided by configuration (see the implementations in {@code be.onboarding.api.messaging}).
 */
public interface OnboardingEventPublisher {

    /** @throws EventPublicationException when the event could not be published */
    void publish(ApplicationSubmitted event);
}
