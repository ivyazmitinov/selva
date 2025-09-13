package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.service.UserService;

import java.net.URI;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/")
public class IndexController {

    private final UserService userService;

    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @Get
    @Transactional
    public HttpResponse<?> index(@Nullable Authentication authentication) {
        if (authentication != null) {
            return HttpResponse.seeOther(URI.create(HomeController.PATH));
        } else {
            return HttpResponse.seeOther(URI.create("/auth/login"));
        }
    }
}
