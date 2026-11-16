package be.onboarding.api.application;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Accepts new onboarding applications.
 *
 * <p>For now it only assigns an id. Persistence, validation and the asynchronous onboarding
 * process are added in later steps.
 */
@Service
public class ApplicationService {

    private final Supplier<UUID> idGenerator;

    public ApplicationService() {
        this(UUID::randomUUID);
    }

    ApplicationService(Supplier<UUID> idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ApplicationResponse submit(ApplicationRequest request) {
        return new ApplicationResponse(idGenerator.get(), ApplicationStatus.RECEIVED);
    }
}
