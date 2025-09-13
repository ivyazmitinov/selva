package org.ivanvyazmitinov.selva.controller.app.interceptor;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.security.authentication.Authentication;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.controller.app.BaseProfileController;
import org.ivanvyazmitinov.selva.controller.app.AppControllerConstants;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.enums.Role;
import org.ivanvyazmitinov.selva.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.PROFILE_ID;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ID;

@ServerFilter(AppControllerConstants.APP_ROOT_PATH + "/**")
public class UnfinishedRegistrationInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(UnfinishedRegistrationInterceptor.class);

    private final UserService userService;

    public UnfinishedRegistrationInterceptor(UserService userService) {
        this.userService = userService;
    }

    @RequestFilter
    @Transactional
    public CompletableFuture<Optional<HttpResponse<?>>> redirectToHomeOnUnfinishedRegistration(
            HttpRequest<?> request,
            Authentication authentication) {
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        var user = userService.fetch(userId).orElseThrow(() -> new IllegalStateException("User %s not found".formatted(userId)));
        if ((user.finishedRegistration() || user.role().equals(Role.ADMIN)) || request.getPath().startsWith(BaseProfileController.PATH)) {
            logger.trace("Proceed with request for user {} ", userId);
            return CompletableFuture.completedFuture(Optional.empty());
        } else {
            logger.trace("User {} has not finished registration, redirecting to profile page", userId);
            return CompletableFuture.completedFuture(Optional.of(HttpResponse.seeOther(URI.create(BaseProfileController.PATH
                    + "/%s".formatted(authentication.getAttributes().get(PROFILE_ID))))));
        }
    }
}
