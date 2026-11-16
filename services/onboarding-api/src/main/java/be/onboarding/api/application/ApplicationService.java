package be.onboarding.api.application;

import java.util.UUID;
import java.util.function.Supplier;

import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Accepts new onboarding applications.
 *
 * <p>For now it only assigns an id. Persistence, validation and the asynchronous onboarding
 * process are added in later steps.
 */
@Service
public class ApplicationService {
    private final static Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private final Supplier<UUID> idGenerator;
    private final KycProperties kycProperties;

    @Autowired
    public ApplicationService(KycProperties kycProperties) {
        this(UUID::randomUUID, kycProperties);
    }

    ApplicationService(Supplier<UUID> idGenerator, KycProperties kycProperties) {
        this.idGenerator = idGenerator;
        this.kycProperties = kycProperties;
    }

    public ApplicationResponse submit(ApplicationRequest request) {
       logger.info("The key: " + kycProperties.apiKey());
        return new ApplicationResponse(idGenerator.get(), ApplicationStatus.RECEIVED);
    }
}
