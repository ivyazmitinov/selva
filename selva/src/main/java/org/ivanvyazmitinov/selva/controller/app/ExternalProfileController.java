package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.service.ExternalProfileService;
import org.ivanvyazmitinov.selva.service.auth.AuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static io.micronaut.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.ivanvyazmitinov.selva.controller.app.ExternalProfileController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.*;

@Secured(USER_ROLE_NAME)
@Controller(PATH)
public class ExternalProfileController {
    public static final String PATH = AppControllerConstants.APP_ROOT_PATH + "/external-profile";
    private final static Logger logger = LoggerFactory.getLogger(ExternalProfileController.class);

    private final ExternalProfileService externalProfileService;

    public ExternalProfileController(ExternalProfileService externalProfileService) {
        this.externalProfileService = externalProfileService;
    }

    @Post(value = "/create-initial", consumes = APPLICATION_FORM_URLENCODED)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> createInitialProfile(Long integrationId, Authentication authentication) {
        logger.debug("Creating external profile for integration {}", integrationId);
        var baseProfileId = (Long) authentication.getAttributes().get(PROFILE_ID);
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        var profileId = externalProfileService.createInitialProfile(baseProfileId, integrationId, userId);
        return HttpResponse.seeOther(URI.create(PATH + "/%s".formatted(profileId)));
    }

    @Get("/{id}")
    @View("external-profile")
    @Transactional
    public Map<String, Object> fetchForEdit(Long id, Authentication authentication) {
        logger.debug("Editing profile {}", id);
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        var profileEditData = externalProfileService.fetchForEdit(id, userId);
        var profileFields = profileEditData
                .externalProfile()
                .fields()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().order()))
                .toList();
        Map<String, Object> result = new HashMap<>(Map.of(
                "externalIntegration", profileEditData.integration(),
                "externalProfile", profileEditData.externalProfile(),
                "baseProfileFields", profileEditData.baseProfile().userFields(),
                "externalProfileFields", profileFields));
        if (profileEditData.integration().logo() != null) {
            result.put("integrationLogo", Base64.getEncoder().encodeToString(profileEditData.integration().logo().bytes()));
        }
        return result;
    }

    @Post("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    public HttpResponse<?> save(@PathVariable Long id,
                                @Body MultipartBody requestBody,
                                 Authentication authentication) {
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        externalProfileService.save(id, userId, requestBody);
        return HttpResponse.seeOther(URI.create(PATH + "/%s".formatted(id)));
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    @Post(value = "/{id}/delete", consumes = MediaType.APPLICATION_FORM_URLENCODED,  produces = MediaType.TEXT_HTML)
    public HttpResponse<?> delete(@PathVariable Long id, Authentication authentication) {
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        externalProfileService.delete(id, userId);
        return HttpResponse.seeOther(URI.create("/"));
    }
}
