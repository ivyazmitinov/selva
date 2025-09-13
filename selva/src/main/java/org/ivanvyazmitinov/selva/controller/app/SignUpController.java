package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.ivanvyazmitinov.selva.model.SignUpForm;
import org.ivanvyazmitinov.selva.service.BaseProfileService;
import org.ivanvyazmitinov.selva.service.auth.app.SignUpService;

import java.net.URI;

import static io.micronaut.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.ivanvyazmitinov.selva.controller.app.AppControllerConstants.AUTH_ROOT_PATH;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(AUTH_ROOT_PATH + "/signup")
public class SignUpController {

    private final SignUpService signUpService;
    private final BaseProfileService baseProfileService;

    public SignUpController(SignUpService signUpService, BaseProfileService baseProfileService) {
        this.signUpService = signUpService;
        this.baseProfileService = baseProfileService;
    }

    @Get
    @View("signup")
    public HttpResponse<?> index() {
        return HttpResponse.ok();
    }

    @Post(consumes = APPLICATION_FORM_URLENCODED)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> signup(@NotNull @Valid @Body SignUpForm signUpForm) {
        var userId = signUpService.signup(signUpForm);
        baseProfileService.createInitialProfile(userId);
        return HttpResponse.seeOther(URI.create("/auth/login?successfulRegistration=true"));
    }
}
