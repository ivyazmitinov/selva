package org.ivanvyazmitinov.selva.controller.app;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.views.View;
import io.micronaut.views.fields.Fieldset;
import io.micronaut.views.fields.FormGenerator;
import io.micronaut.views.fields.elements.InputDateFormElement;
import io.micronaut.views.fields.elements.InputFileFormElement;
import io.micronaut.views.fields.elements.InputFormElement;
import io.micronaut.views.fields.elements.InputTextFormElement;
import io.micronaut.views.fields.messages.Message;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.UserField;
import org.ivanvyazmitinov.selva.model.UserFieldRawValue;
import org.ivanvyazmitinov.selva.service.BaseProfileService;
import org.ivanvyazmitinov.selva.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.ivanvyazmitinov.selva.controller.app.BaseProfileController.PATH;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ID;
import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.USER_ROLE_NAME;

@Secured(USER_ROLE_NAME)
@Controller(PATH)
public class BaseProfileController {
    public static final String PATH = AppControllerConstants.APP_ROOT_PATH + "/base-profile";
    private final static Logger logger = LoggerFactory.getLogger(BaseProfileController.class);
    public static final String UNSUPPORTED_VALUE_TYPE_ERROR = "Field %s is invalid: references are not supported in a base profile.";
    private final FormGenerator formGenerator;
    private final BaseProfileService baseProfileService;
    private final UserService userService;


    public BaseProfileController(FormGenerator formGenerator,
                                 BaseProfileService baseProfileService,
                                 UserService userService) {
        this.formGenerator = formGenerator;
        this.baseProfileService = baseProfileService;
        this.userService = userService;
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
        var user = userService.fetch(userId).orElseThrow();
        baseProfileService.storeFields(id, userId, requestBody);
        userService.markRegistrationFinished(userId);
        if (user.finishedRegistration()) {
            return HttpResponse.seeOther(URI.create("%s/%d".formatted(PATH, id)));
        } else {
            return HttpResponse.seeOther(URI.create("/"));
        }
    }

    @Get(value = "/{id}", produces = MediaType.TEXT_HTML)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @View("base-profile")
    @Transactional
    public Map<String, Object> fetchForEdit(@PathVariable Long id, Authentication authentication) {
        var userId = (Long) authentication.getAttributes().get(USER_ID);
        var baseProfile = baseProfileService.fetch(id, userId);
        var user = userService.fetch(userId).orElseThrow();
        var userFields = baseProfile.userFields()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().order()))
                .toList();
        return Map.of(
                "baseProfileId", baseProfile.id(),
                "userFields", userFields,
                "registrationFinished", user.finishedRegistration());
    }
}