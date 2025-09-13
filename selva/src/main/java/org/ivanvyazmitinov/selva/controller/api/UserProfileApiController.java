package org.ivanvyazmitinov.selva.controller.api;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.ExternalApiUserProfiles;
import org.ivanvyazmitinov.selva.service.ExternalProfileService;
import org.ivanvyazmitinov.selva.service.auth.AuthConstants;

import static io.micronaut.http.MediaType.APPLICATION_JSON;
import static org.ivanvyazmitinov.selva.controller.api.ApiControllerConstants.API_ROOT_PATH;
import static org.ivanvyazmitinov.selva.controller.api.UserProfileApiController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.API_INTEGRATION_ROLE_NAME;

@Secured(API_INTEGRATION_ROLE_NAME)
@Controller(PATH)
public class UserProfileApiController {
    public static final String PATH = API_ROOT_PATH + "/user-profile";
    private final ExternalProfileService externalProfileService;

    public UserProfileApiController(ExternalProfileService externalProfileService) {
        this.externalProfileService = externalProfileService;
    }


    @Get(value = "/{id}", produces = APPLICATION_JSON)
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    public ExternalApiUserProfiles listUserProfiles(@PathVariable("id") Long userId,
                                                    Authentication authentication) {
        return externalProfileService.fetchUserProfiles(userId,
                ((Long) authentication.getAttributes().get(AuthConstants.INTEGRATION_ID)));
    }
}
