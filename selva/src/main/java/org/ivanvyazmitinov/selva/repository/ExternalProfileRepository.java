package org.ivanvyazmitinov.selva.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.*;
import org.ivanvyazmitinov.selva.repository.utils.ImageConverter;
import org.ivanvyazmitinov.selva.service.FileDownloadService;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record9;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.ivanvyazmitinov.selva.repository.RepositorySessionConstants.SESSION_INTEGRATION_ID_KEY;
import static org.ivanvyazmitinov.selva.repository.RepositorySessionConstants.SESSION_USER_ID_KEY;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.*;

@Singleton
public class ExternalProfileRepository {
    private final static Logger log = LoggerFactory.getLogger(ExternalProfileRepository.class);
    public static final TypeReference<Map<String, UserField>> FIELDS_TYPE_REFERENCE = new TypeReference<>() {
    };
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final ImageConverter imageConverter;
    private final FileDownloadService fileDownloadService;

    public ExternalProfileRepository(DSLContext dsl,
                                     ObjectMapper objectMapper,
                                     ImageConverter imageConverter,
                                     FileDownloadService fileDownloadService) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
        this.imageConverter = imageConverter;
        this.fileDownloadService = fileDownloadService;
    }

    public long create(long baseProfileId, Long integrationId, Long userId, Map<String, UserField> userFields) {
        try {
            dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
            var externalProfileRecord = dsl.newRecord(EXTERNAL_PROFILE);
            externalProfileRecord.setBaseProfileId(baseProfileId);
            externalProfileRecord.setExternalIntegrationId(integrationId);
            externalProfileRecord.setIsPublic(false);
            externalProfileRecord.setFields(JSONB.valueOf(objectMapper.writeValueAsString(userFields)));
            externalProfileRecord.store();
            return externalProfileRecord.getId();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<ExternalProfile> fetchById(long id, Long userId) {
        dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        return dsl.selectFrom(EXTERNAL_PROFILE)
                .where(EXTERNAL_PROFILE.ID.eq(id))
                .fetchOptional(r -> new ExternalProfile(
                        r.get(EXTERNAL_PROFILE.ID),
                        r.get(EXTERNAL_PROFILE.BASE_PROFILE_ID),
                        r.get(EXTERNAL_PROFILE.EXTERNAL_INTEGRATION_ID),
                        r.get(EXTERNAL_PROFILE.IS_PUBLIC),
                        parseFields(r.get(EXTERNAL_PROFILE.FIELDS))));
    }

    public Optional<ExternalProfileEditData> fetchForEdit(Long externalProfileId, Long userId) {
        dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        return dsl.select(EXTERNAL_PROFILE.externalIntegration().ID,
                        EXTERNAL_PROFILE.externalIntegration().NAME,
                        EXTERNAL_PROFILE.externalIntegration().LOGO,
                        EXTERNAL_PROFILE.baseProfile().ID,
                        EXTERNAL_PROFILE.baseProfile().FIELDS,
                        EXTERNAL_PROFILE.ID,
                        EXTERNAL_PROFILE.BASE_PROFILE_ID,
                        EXTERNAL_PROFILE.IS_PUBLIC,
                        EXTERNAL_PROFILE.FIELDS
                )
                .from(EXTERNAL_PROFILE)
                .innerJoin(EXTERNAL_INTEGRATION)
                .on(EXTERNAL_PROFILE.EXTERNAL_INTEGRATION_ID.eq(EXTERNAL_INTEGRATION.ID))
                .where(EXTERNAL_PROFILE.ID.eq(externalProfileId))
                .fetchOptional(this::prepareExternalProfileEditData);
    }

    private ExternalProfileEditData prepareExternalProfileEditData(Record9<Long, String, byte[], Long, JSONB, Long, Long, Boolean, JSONB> r) {
        var externalIntegrationId = r.get(EXTERNAL_PROFILE.externalIntegration().ID);
        var integration = new ExternalIntegration(
                externalIntegrationId,
                r.get(EXTERNAL_PROFILE.externalIntegration().NAME),
                null,
                imageConverter.convert(r.get(EXTERNAL_PROFILE.externalIntegration().LOGO)).orElse(null),
                null
        );
        var profile = new ExternalProfile(
                r.get(EXTERNAL_PROFILE.ID),
                r.get(EXTERNAL_PROFILE.BASE_PROFILE_ID),
                externalIntegrationId,
                r.get(EXTERNAL_PROFILE.IS_PUBLIC),
                parseFields(r.get(EXTERNAL_PROFILE.FIELDS)));
        var baseProfileFields = parseFields(r.get(EXTERNAL_PROFILE.baseProfile().FIELDS));
        // Load file names for dropdowns
        baseProfileFields = baseProfileFields
                .entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    var userField = e.getValue();
                    if (userField.type().equals(UserFieldType.FILE)) {
                        var userFieldValue = (UserFieldRawValue) userField.value();
                        if (userFieldValue == null) {
                            return userField;
                        }
                        var fileId = (Integer) userFieldValue.value();
                        var fileName = dsl.select(FILE.FILE_NAME)
                                .from(FILE)
                                .where(FILE.ID.eq(fileId.longValue()))
                                .fetchOne(FILE.FILE_NAME);
                        return new UserField(userField.name(),
                                userField.order(),
                                userField.type(),
                                new UserFieldRawValue(fileName));

                    } else {
                        return userField;
                    }
                }));
        var baseProfile = new BaseProfile(r.get(EXTERNAL_PROFILE.baseProfile().ID),
                baseProfileFields);
        return new ExternalProfileEditData(integration, baseProfile, profile);
    }

    private Map<String, UserField> parseFields(JSONB f) {
        try {
            return objectMapper.readValue(f.data(), FIELDS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Long id, Long userId, Boolean isPublic, Map<String, UserFieldValue> fieldValues) {
        var fieldUpdateMap = fieldValues.entrySet()
                .stream()
                .map((e) -> {
                    try {
                        var jsonField = DSL.field("{0}['%s']['value']".formatted(e.getKey()), EXTERNAL_PROFILE.FIELDS);
                        var value = JSONB.valueOf(objectMapper.writeValueAsString(e.getValue()));
                        return Map.entry(jsonField, value);
                    } catch (JsonProcessingException exception) {
                        throw new RuntimeException(exception);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        dsl.update(EXTERNAL_PROFILE)
                .set(fieldUpdateMap)
                .set(EXTERNAL_PROFILE.IS_PUBLIC, isPublic)
                .where(EXTERNAL_PROFILE.ID.eq(id))
                .execute();
    }

    public ExternalApiUserProfiles findByUserIdForExternalApi(Long userId, Long currentIntegrationId) {
        dsl.setLocal(SESSION_INTEGRATION_ID_KEY, DSL.val(currentIntegrationId)).execute();
        var fieldsFlat = DSL.table("jsonb_each(?)", EXTERNAL_PROFILE.FIELDS).as("f");
        var fieldContent = DSL.field(DSL.name(fieldsFlat.getName(), "value"), JSONB.class);
        var fieldName = PostgresDSL.jsonbGetAttributeAsText(fieldContent, "name");
        var fieldValue = PostgresDSL.jsonbGetAttribute(fieldContent, "value");
        var baseProfileFieldId = PostgresDSL.jsonbGetAttributeAsText(fieldValue, "fieldId");
        var baseProfileFieldValue = PostgresDSL.jsonbGetAttribute(BASE_PROFILE.FIELDS, baseProfileFieldId);
        var resultingField =
                DSL.when(PostgresDSL.jsonbGetAttributeAsText(fieldValue, "@type").eq("reference"),
                                baseProfileFieldValue)
                        .else_(fieldContent);
        var fieldsPerIntegration = dsl.select(EXTERNAL_INTEGRATION.ID,
                        EXTERNAL_INTEGRATION.NAME,
                        fieldName,
                        resultingField)
                .from(EXTERNAL_PROFILE).innerJoin(EXTERNAL_INTEGRATION)
                .on(EXTERNAL_PROFILE.EXTERNAL_INTEGRATION_ID.eq(EXTERNAL_INTEGRATION.ID))
                .innerJoin(BASE_PROFILE).on(BASE_PROFILE.ID.eq(EXTERNAL_PROFILE.BASE_PROFILE_ID))
                .crossJoin(fieldsFlat)
                .where(BASE_PROFILE.USER_ID.eq(userId))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(r -> r.get(EXTERNAL_INTEGRATION.NAME)));
        ExternalApiUserProfile current = null;
        var otherIntegrations = new ArrayList<ExternalApiUserProfile>();
        for (var entry : fieldsPerIntegration.entrySet()) {
            var fields = entry.getValue()
                    .stream()
                    .collect(Collectors.toMap(r -> r.get(fieldName), r -> {
                        try {
                            var jsonb = r.get(resultingField);
                            if (jsonb == null) {
                                return new ExternalApiUserField(null);
                            }
                            var userField = objectMapper.readValue(jsonb.data(), UserField.class);
                            var fieldRawValue = (UserFieldRawValue) userField.value();
                            var value = userField.type().equals(UserFieldType.FILE) ?
                                    fileDownloadService.prepareFileDownload(((Integer) fieldRawValue.value()).longValue())
                                    : fieldRawValue.value();
                            return new ExternalApiUserField(value);
                        } catch (JsonProcessingException ex) {
                            throw new RuntimeException(ex);
                        }
                    }));
            var externalApiUserProfile = new ExternalApiUserProfile(entry.getKey(), fields);
            var integrationId = entry.getValue().getFirst().get(EXTERNAL_INTEGRATION.ID);
            if (integrationId.equals(currentIntegrationId)) {
                current = externalApiUserProfile;
            } else {
                otherIntegrations.add(externalApiUserProfile);
            }
        }
        return new ExternalApiUserProfiles(current, otherIntegrations);
    }

    public void delete(Long id, Long currentUserId) {
        dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(currentUserId)).execute();
        dsl.deleteFrom(EXTERNAL_PROFILE).where(EXTERNAL_PROFILE.ID.eq(id)).execute();
    }
}
