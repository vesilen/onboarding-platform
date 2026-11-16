package be.onboarding.api.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.xml.xpath.XPathEvaluationResult;

/** Version 1 of the onboarding API. Returns 202 Accepted: onboarding itself is a long-running process. */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApplicationResponse submit(@RequestBody ApplicationRequest request) {
        return applicationService.submit(request);
    }
}
