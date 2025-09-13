package org.ivanvyazmitinov.selva.service;

import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.*;
import org.ivanvyazmitinov.selva.repository.ExternalIntegrationRepository;
import org.ivanvyazmitinov.selva.repository.ExternalProfileRepository;
import org.ivanvyazmitinov.selva.service.utils.CompletedPartUnpacker;
import org.ivanvyazmitinov.selva.service.utils.UserFieldBuilder;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ExternalProfileService {
    private final ExternalIntegrationRepository externalIntegrationRepository;
    private final ExternalProfileRepository externalProfileRepository;
    private final UserFieldBuilder userFieldBuilder;
    private final CompletedPartUnpacker completedPartUnpacker;

    public ExternalProfileService(ExternalIntegrationRepository externalIntegrationRepository,
                                  ExternalProfileRepository externalProfileRepository,
                                  UserFieldBuilder userFieldBuilder,
                                  CompletedPartUnpacker completedPartUnpacker) {
        this.externalIntegrationRepository = externalIntegrationRepository;
        this.externalProfileRepository = externalProfileRepository;
        this.userFieldBuilder = userFieldBuilder;
        this.completedPartUnpacker = completedPartUnpacker;
    }

    public long createInitialProfile(Long baseProfileId, Long integrationId, Long userId) {
        var externalIntegration = externalIntegrationRepository.findByIdLite(integrationId)
                .orElseThrow(() -> new IllegalStateException("Integration \"%s\" not found".formatted(integrationId)));
        var userFields = externalIntegration.profileTemplate();
        var userFieldMap = userFields
                .values()
                .stream()
                .collect(Collectors.toMap(f -> UUID.randomUUID().toString(), Function.identity()));
        return externalProfileRepository.create(baseProfileId, integrationId, userId, userFieldMap);
    }


    public ExternalProfileEditData fetchForEdit(Long profileId, Long userId) {
        return externalProfileRepository.fetchForEdit(profileId, userId)
                .orElseThrow(() -> new IllegalStateException("Profile \"%s\" not found".formatted(profileId)));
    }

    public void save(Long id, Long userId, MultipartBody requestBody) {
        var requestFields = Flux.from(requestBody)
                .collectMap(CompletedPart::getName, completedPartUnpacker::unpackCompletedPart)
                .block();
        var isPublic = requestFields.remove("is-public") != null;
        var externalProfile = externalProfileRepository.fetchById(id, userId).orElseThrow();
        var newFieldValues = requestFields
                .entrySet()
                .stream()
                .map(e -> prepareFieldValue(externalProfile, e))
                .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2));
        externalProfileRepository.update(id, userId, isPublic, newFieldValues);
    }

    private Tuple2<String, UserFieldValue> prepareFieldValue(ExternalProfile externalProfile,
                                                    Map.Entry<String, FormFieldValue> formFields) {
        var fieldId = formFields.getKey().replace("__reference", "");
        var userFieldValue = formFields.getKey().endsWith("__reference")
                ?
                new UserFieldBaseProfileValueReference(new String(formFields.getValue().content()))
                : userFieldBuilder.prepareFieldValue(formFields.getValue());
        return Tuples.of(fieldId, userFieldValue);
    }

    public ExternalApiUserProfiles fetchUserProfiles(
            Long userId,
            Long currentIntegrationId) {
        return externalProfileRepository.findByUserIdForExternalApi(userId, currentIntegrationId);
    }

    public void delete(Long id, Long currentUserId) {
        externalProfileRepository.delete(id, currentUserId);
    }
}
