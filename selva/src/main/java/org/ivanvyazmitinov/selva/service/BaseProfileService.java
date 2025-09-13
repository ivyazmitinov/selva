package org.ivanvyazmitinov.selva.service;

import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.http.server.multipart.MultipartBody;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.*;
import org.ivanvyazmitinov.selva.repository.BaseProfileRepository;
import org.ivanvyazmitinov.selva.service.utils.CompletedPartUnpacker;
import org.ivanvyazmitinov.selva.service.utils.UserFieldBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class BaseProfileService {

    private final static Logger log = LoggerFactory.getLogger(BaseProfileService.class);
    private final BaseProfileRepository baseProfileRepository;
    private final UserFieldBuilder userFieldBuilder;
    private final CompletedPartUnpacker completedPartUnpacker;

    public BaseProfileService(BaseProfileRepository baseProfileRepository,
                              UserFieldBuilder userFieldBuilder, CompletedPartUnpacker completedPartUnpacker) {
        this.baseProfileRepository = baseProfileRepository;
        this.userFieldBuilder = userFieldBuilder;
        this.completedPartUnpacker = completedPartUnpacker;
    }

    @Transactional
    public void storeFields(Long id, Long userId, MultipartBody requestBody) {
        Mono.justOrEmpty(baseProfileRepository.fetchProfile(id, userId))
                .switchIfEmpty(Mono.error(new IllegalStateException("No profile found for id " + id)))
                .flatMap(baseProfile -> Mono.just(baseProfile.userFields())
                        .flatMap(existingFields -> updateFields(requestBody, baseProfile, userId, existingFields)))
                .block();
    }

    private Mono<Long> updateFields(MultipartBody requestBody, BaseProfile baseProfile, Long userId, Map<String, UserField> existingFields) {
        return Flux.from(requestBody)
                // Group by extracted field id
                .groupBy(this::extractFieldId)
                .flatMap(fieldInfo -> createOrUpdateField(existingFields, fieldInfo))
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .map(newFields -> prepareUpdatedFields(existingFields, newFields))
                .doOnNext(newFields -> baseProfileRepository.updateProfileFields(baseProfile.id(), userId, newFields))
                .then(Mono.just(baseProfile.id()));
    }

    private Map<String, UserField> prepareUpdatedFields(
            Map<String, UserField> existingFields,
            Map<String, UserField> newFields) {
        var updatedFields = new HashMap<>(existingFields);
        existingFields
                .keySet()
                .forEach(fieldId -> {
                    if (!newFields.containsKey(fieldId)) {
                        updatedFields.remove(fieldId);
                    }
                });
        updatedFields.putAll(newFields);
        return updatedFields;
    }

    private String extractFieldId(CompletedPart completedPart) {
        return completedPart.getName().split("__")[0];
    }

    private Mono<Tuple2<String, UserField>> createOrUpdateField(Map<String, UserField> existingFields,
                                                                GroupedFlux<String, CompletedPart> fieldInfo) {
        var requestFieldId = fieldInfo.key();
        var existingField = Optional.ofNullable(existingFields.get(requestFieldId));
        return fieldInfo.collectMap(CompletedPart::getName, completedPartUnpacker::unpackCompletedPart)
                .handle((parts, sink) -> {
                    var fieldLabel = existingField.map(UserField::name)
                            .orElseGet(() -> sanitizeLabel(new String(parts.get(requestFieldId + "__label").content())));
                    var fieldType = existingField.map(UserField::type)
                            .orElseGet(() -> UserFieldType.valueOf(new String(parts.get(requestFieldId + "__type").content()).toUpperCase()));
                    var fieldOrder = Integer.valueOf(new String(parts.get(requestFieldId + "__order").content()));
                    // Only the new field value is overwritten
                    UserFieldValue fieldValue = userFieldBuilder.prepareFieldValue(parts.get(requestFieldId + "__value"));
                    if (fieldValue == null) {
                        fieldValue = existingField.map(UserField::value).orElse(null);
                    }
                    var fieldId = existingField.isEmpty() ? UUID.randomUUID().toString() : requestFieldId;
                    sink.next(Tuples.of(fieldId, new UserField(fieldLabel, fieldOrder, fieldType, fieldValue)));
                });
    }

    private String sanitizeLabel(String label) {
        label = label.trim();
        if (label.endsWith(":")) {
            label = label.substring(0, label.length() - 1);
        }
        return label;
    }

    public void createInitialProfile(long userId) {
        log.debug("Preparing new profile for user {}", userId);
        var initialFields = Map.of(UUID.randomUUID().toString(),
                new UserField("Name", 0, UserFieldType.TEXT, new UserFieldRawValue("")),
                UUID.randomUUID().toString(),
                new UserField("Surname", 1, UserFieldType.TEXT, new UserFieldRawValue("")));
        baseProfileRepository.create(userId, initialFields);
    }

    public BaseProfile fetch(long id, Long userId) {
        log.debug("Loading profile for {}", id);
        var baseProfile = baseProfileRepository.fetchProfile(id, userId);
        if (baseProfile.isEmpty()) {
            log.debug("No base profile record found for id {}", id);
            throw new NotFoundException();
        }
        return baseProfile.get();
    }

}
