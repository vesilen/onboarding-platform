package be.onboarding.api.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Accepts new onboarding applications: assigns an id and publishes an {@link ApplicationSubmitted}
 * event. The onboarding itself continues asynchronously in onboarding-processor.
 *
 * <p>Persistence and validation are added in later steps. Once there is a database, saving and
 * publishing must become atomic (transactional outbox).
 */
@Service
public class ApplicationService {

    private final Supplier<UUID> idGenerator;
    private final OnboardingEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ApplicationService(OnboardingEventPublisher eventPublisher, Clock clock) {
        this(UUID::randomUUID, eventPublisher, clock);
    }

    ApplicationService(Supplier<UUID> idGenerator, OnboardingEventPublisher eventPublisher, Clock clock) {
        this.idGenerator = idGenerator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * @throws EventPublicationException when the event could not be published; the application is
     *     then NOT accepted and the caller should retry
     */
    public ApplicationResponse submit(ApplicationRequest request) {
        var applicationId = idGenerator.get();
        eventPublisher.publish(new ApplicationSubmitted(
                applicationId, request.firstName(), request.lastName(), request.email(), Instant.now(clock)));
        return new ApplicationResponse(applicationId, ApplicationStatus.RECEIVED);
    }
}
