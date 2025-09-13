package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.context.annotation.Context;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import io.micronaut.views.fields.messages.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ivanvyazmitinov.selva.controller.app.AppControllerConstants.AUTH_ROOT_PATH;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(AUTH_ROOT_PATH + "/login")
@Context
public class LoginController {

    @Get
    @View("login")
    public Map<String, Object> index(@QueryValue Optional<Boolean> failure,
                                     @QueryValue Optional<Boolean> successfulRegistration) {
        if (failure.isPresent() && failure.get()) {
            return Map.of("errors", List.of(new Message("Login failed, please try again", null)));
        } else if (successfulRegistration.isPresent()) {
            return Map.of("successfulRegistration", true);
        }
        else {
            return Map.of();
        }
    }
}
