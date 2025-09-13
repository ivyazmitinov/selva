package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.session.Session;
import io.micronaut.views.View;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.ExternalIntegration;
import org.ivanvyazmitinov.selva.model.UserField;
import org.ivanvyazmitinov.selva.service.ExternalIntegrationService;

import java.net.URI;
import java.util.*;

import static org.ivanvyazmitinov.selva.controller.app.ExternalIntegrationManagementController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.ADMIN_ROLE_NAME;

@Secured(ADMIN_ROLE_NAME)
@Controller(PATH)
public class ExternalIntegrationManagementController {
    public static final String PATH = AppControllerConstants.APP_ROOT_PATH + "/external-integration";
    private final ExternalIntegrationService externalIntegrationService;

    public ExternalIntegrationManagementController(ExternalIntegrationService externalIntegrationService) {
        this.externalIntegrationService = externalIntegrationService;
    }

    @Get("/{id}")
    @View("external-integration")
    @Transactional
    public Map<String, Object> edit(Session session, @PathVariable String id) {
        if (id.equals("new")) {
            return Map.of();
        }
        var integration = externalIntegrationService.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalStateException("Integration %s not found".formatted(id)));
        List<Map.Entry<String, UserField>> fields = integration.profileTemplate()
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getValue().order()))
                .toList();
        Map<String, Object> context = new HashMap<>();
        context.put("externalIntegration", integration);
        context.put("externalIntegrationFields", fields);
        var apiTokenSessionKey = integration.id() + "__api_token";
        var apiToken = session.get(apiTokenSessionKey);
        if (apiToken.isPresent()) {
            context.put("apiKey", apiToken.get());
            session.remove(apiTokenSessionKey);
        }
        if (integration.logo() != null) {
            context.put("integrationLogo", Base64.getEncoder().encodeToString(integration.logo().bytes()));
        }
        return context;
    }

    @Post(value = "{/id}", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_HTML)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> save(@PathVariable @Nullable Long id,
                                @Body MultipartBody requestBody,
                                Session session) {
        ExternalIntegration integration;
        if (id == null) {
            integration = externalIntegrationService.create(requestBody);
            session.put(integration.id() + "__api_token", integration.token());
        } else {
            integration = externalIntegrationService.update(id, requestBody);
        }
        return HttpResponse.seeOther(URI.create("%s/%d".formatted(PATH, integration.id())));
    }

    @Post(value = "/{id}/update-token", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_HTML)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> updateToken(@PathVariable Long id, Session session) {
        var newToken = externalIntegrationService.updateToken(id);
        session.put(id + "__api_token", newToken);
        return HttpResponse.seeOther(URI.create("%s/%d".formatted(PATH, id)));
    }

    @Post(value = "/{id}/delete", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.TEXT_HTML)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> delete(@PathVariable Long id) {
        externalIntegrationService.delete(id);
        return HttpResponse.seeOther(URI.create("/"));
    }
}
