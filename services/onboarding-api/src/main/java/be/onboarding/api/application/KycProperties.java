package be.onboarding.api.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.kyc")
public record KycProperties(String apiKey) {}
