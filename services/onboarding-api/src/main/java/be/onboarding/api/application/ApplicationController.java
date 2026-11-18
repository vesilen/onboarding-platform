package be.onboarding.api.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    /** Messaging is unavailable: tell the channel to retry instead of returning a 202 that is not true. */
    @ExceptionHandler(EventPublicationException.class)
    ProblemDetail handlePublicationFailure(EventPublicationException e) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The application could not be accepted right now, please retry.");
        problem.setTitle("Service Unavailable");
        return problem;
    }
}
