package org.ivanvyazmitinov.selva.service;

import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.model.*;
import org.ivanvyazmitinov.selva.repository.ExternalIntegrationRepository;
import org.ivanvyazmitinov.selva.service.auth.app.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ExternalIntegrationService {
    private final ExternalIntegrationRepository externalIntegrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;

    public ExternalIntegrationService(ExternalIntegrationRepository externalIntegrationRepository,
                                      PasswordEncoder passwordEncoder, TokenGenerator tokenGenerator) {
        this.externalIntegrationRepository = externalIntegrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
    }

    public Optional<ExternalIntegration> findById(Long id) {
        return externalIntegrationRepository.findById(id);
    }

    public List<ExternalIntegrationOverview> fetchAllOverview(@Nullable Long baseProfileId, @Nonnull Long currentUserId) {
        return externalIntegrationRepository.fetchAllOverview(currentUserId, baseProfileId);
    }

    public ExternalIntegration create(@Nonnull MultipartBody requestBody) {
        return save(null, requestBody);
    }

    public ExternalIntegration update(Long id, @Nonnull MultipartBody requestBody) {
        return save(id, requestBody);
    }

    public String updateToken(Long id) {
        var integration = externalIntegrationRepository.findByIdLite(id)
                .orElseThrow();
        var tokenRaw = tokenGenerator.generateToken(integration.id().toString());
        var tokenEncoded = passwordEncoder.encode(tokenRaw);
        externalIntegrationRepository.updateToken(id, tokenEncoded);
        return tokenRaw;
    }

    private ExternalIntegration save(@Nullable Long id, @Nonnull MultipartBody requestBody) {
        var existingFields = (id == null ?
                Map.<String, UserField>of() :
                externalIntegrationRepository.findByIdLite(id)
                        .orElseThrow()
                        .profileTemplate());
        var maxExistingOrder = existingFields.values().stream()
                .mapToInt(UserField::order)
                .max()
                .orElse(-1);
        var inputFields = Flux.from(requestBody)
                .collectMap(CompletedPart::getName, this::unpackCompletedPart)
                .block();
        var integrationName = new String(inputFields.remove("name"));
        var integrationLogo = inputFields.remove("logo");
        Map<String, UserField> requestFields = Flux.fromIterable(inputFields.entrySet())
                .groupBy(e -> this.extractFieldId(e.getKey()))
                .flatMap(fieldInfo -> createField(maxExistingOrder, fieldInfo))
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .block();
        var fieldsToSave = new HashMap<>(existingFields);
        Set<String> deletedFields = fieldsToSave.keySet()
                .stream()
                .filter(key -> !requestFields.containsKey(key))
                .collect(Collectors.toSet());
        fieldsToSave.keySet().removeAll(deletedFields);
        requestFields.entrySet().forEach(entry -> {
            var key = fieldsToSave.containsKey(entry.getKey()) ?
                    entry.getKey() :
                    UUID.randomUUID().toString();
            fieldsToSave.put(key, entry.getValue());
        });
        if (id == null) {
            var newId = externalIntegrationRepository.reserveNextId();
            var tokenRaw = tokenGenerator.generateToken(newId.toString());
            var tokenEncoded = passwordEncoder.encode(tokenRaw);
            ExternalIntegration integration = externalIntegrationRepository.create(newId,
                    integrationName,
                    integrationLogo,
                    tokenEncoded,
                    fieldsToSave);
            return new ExternalIntegration(integration.id(),
                    integration.name(),
                    tokenRaw,
                    integration.logo(),
                    integration.profileTemplate()
            );
        } else {
            return externalIntegrationRepository.update(id, integrationName, integrationLogo, fieldsToSave);
        }
    }

    private Mono<Tuple2<String, UserField>> createField(int maxExistingOrder,
                                                        GroupedFlux<String, Map.Entry<String, byte[]>> fieldInfo) {
        var fieldId = fieldInfo.key();
        return fieldInfo.collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .handle((parts, sink) -> {
                    var fieldLabel = sanitizeLabel(new String(parts.get(fieldId + "__label")));
                    var fieldType = UserFieldType.valueOf(new String(parts.get(fieldId + "__type")).toUpperCase());
                    var fieldOrder = Integer.parseInt(new String(parts.get(fieldId + "__order")));
                    sink.next(Tuples.of(fieldId, new UserField(fieldLabel, fieldOrder, fieldType, null)));
                });
    }


    private String extractFieldId(String name) {
        return name.split("__")[0];
    }

    private byte[] unpackCompletedPart(CompletedPart cp) {
        try {
            return cp.getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sanitizeLabel(String label) {
        label = label.trim();
        if (label.endsWith(":")) {
            label = label.substring(0, label.length() - 1);
        }
        return label;
    }

    @Transactional
    public Optional<ExternalIntegrationLite> findByToken(Long integrationId, String token) {
        return externalIntegrationRepository.findByIdLite(integrationId)
                .filter(e -> passwordEncoder.matches(token, e.token()));
    }

    public void delete(Long id) {
        externalIntegrationRepository.delete(id);
    }
}
