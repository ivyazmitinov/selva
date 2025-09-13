package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.ExternalIntegration;
import org.ivanvyazmitinov.selva.model.ExternalIntegrationOverview;
import org.ivanvyazmitinov.selva.service.ExternalIntegrationService;
import org.ivanvyazmitinov.selva.service.auth.AuthConstants;

import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ivanvyazmitinov.selva.controller.app.HomeController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ID;

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller(PATH)
public class HomeController {
    public static final String PATH = AppControllerConstants.APP_ROOT_PATH + "/home";

    private final ExternalIntegrationService externalIntegrationService;

    public HomeController(ExternalIntegrationService externalIntegrationService) {
        this.externalIntegrationService = externalIntegrationService;
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Get
    @View("home")
    @Transactional
    public Map<String, Object> index(Authentication authentication) {
        var baseProfileId = (Long) authentication.getAttributes().get(AuthConstants.PROFILE_ID);
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        var externalIntegrations = externalIntegrationService.fetchAllOverview(baseProfileId, userId);
        var logosBase64 = externalIntegrations.stream()
                .map(ExternalIntegrationOverview::integration)
                .filter(ei -> ei.logo() != null)
                .collect(Collectors.toMap(ExternalIntegration::id, this::convertLogoToBase64));
        return Map.of("integrations", externalIntegrations,
                "logos", logosBase64);
    }

    private String convertLogoToBase64(ExternalIntegration e) {
        return Base64.getEncoder().encodeToString(e.logo().bytes());
    }
}
