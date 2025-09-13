package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.handlers.LogoutHandler;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ivanvyazmitinov.selva.controller.app.AccountController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ID;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ROLE_NAME;

@Controller(PATH)
@Secured(USER_ROLE_NAME)
public class AccountController {
    public static final String PATH = AppControllerConstants.APP_ROOT_PATH + "/account";
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final UserService userService;
    private final LogoutHandler<HttpRequest<?>, MutableHttpResponse<?>> logoutHandler;

    public AccountController(UserService userService, LogoutHandler<HttpRequest<?>, MutableHttpResponse<?>> logoutHandler) {
        this.userService = userService;
        this.logoutHandler = logoutHandler;
    }

    @Post(consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.TEXT_HTML)
    @Transactional
    public HttpResponse<?> deleteAccount(HttpRequest<?> request, Authentication authentication) {
        log.debug("Delete account request");
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        userService.delete(userId);
        return logoutHandler.logout(request);
    }
}
