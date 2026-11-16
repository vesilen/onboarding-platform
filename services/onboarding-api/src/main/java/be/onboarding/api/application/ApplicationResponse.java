package be.onboarding.api.application;

import java.util.UUID;

/** Acknowledgement returned to the channel: the application is accepted, processing continues asynchronously. */
public record ApplicationResponse(UUID applicationId, ApplicationStatus status) {}
