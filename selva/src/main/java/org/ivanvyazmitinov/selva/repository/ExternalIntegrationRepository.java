package org.ivanvyazmitinov.selva.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.ExternalIntegration;
import org.ivanvyazmitinov.selva.model.ExternalIntegrationLite;
import org.ivanvyazmitinov.selva.model.ExternalIntegrationOverview;
import org.ivanvyazmitinov.selva.model.UserField;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Sequences;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.tables.records.ExternalIntegrationRecord;
import org.ivanvyazmitinov.selva.repository.utils.ImageConverter;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record4;
import org.jooq.UpdateSetMoreStep;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ivanvyazmitinov.selva.repository.RepositorySessionConstants.SESSION_USER_ID_KEY;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.EXTERNAL_INTEGRATION;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.EXTERNAL_PROFILE;

@Singleton
public class ExternalIntegrationRepository {
    public static final TypeReference<Map<String, UserField>> FIELDS_TYPE_REFERENCE = new TypeReference<>() {
    };
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final ImageConverter imageConverter;

    public ExternalIntegrationRepository(DSLContext dsl,
                                         ObjectMapper objectMapper,
                                         ImageConverter imageConverter) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
        this.imageConverter = imageConverter;
    }

    /**
     * Return an ExternalIntegration, optionally with logo bytes
     *
     * @param id name of the integration
     * @return {@link ExternalIntegration} with logo
     */
    public Optional<ExternalIntegrationLite> findByIdLite(Long id) {
        return dsl.select(EXTERNAL_INTEGRATION.ID,
                        EXTERNAL_INTEGRATION.NAME,
                        EXTERNAL_INTEGRATION.TOKEN,
                        EXTERNAL_INTEGRATION.PROFILE_TEMPLATE)
                .from(EXTERNAL_INTEGRATION)
                .where(EXTERNAL_INTEGRATION.ID.eq(id))
                .fetchOptional(this::convertToLightDto);
    }

    public Optional<ExternalIntegration> findById(Long id) {
        return dsl.selectFrom(EXTERNAL_INTEGRATION)
                .where(EXTERNAL_INTEGRATION.ID.eq(id))
                .fetchOptional(this::covertToDto);
    }

    public Long reserveNextId() {
        return dsl.nextval(Sequences.EXTERNAL_INTEGRATION_ID_SEQ);
    }

    private ExternalIntegration covertToDto(ExternalIntegrationRecord externalIntegrationRecord) {
        return new ExternalIntegration(externalIntegrationRecord.getId(),
                externalIntegrationRecord.getName(),
                externalIntegrationRecord.getToken(),
                imageConverter.convert(externalIntegrationRecord.getLogo()).orElse(null),
                parseFields(externalIntegrationRecord.getProfileTemplate()));
    }


    private ExternalIntegrationLite convertToLightDto(Record4<Long, String, String, JSONB> r) {
        return new ExternalIntegrationLite(r.component1(),
                r.component2(),
                r.component3(),
                parseFields(r.component4()));
    }

    private Map<String, UserField> parseFields(JSONB fields) {
        try {
            return objectMapper.readValue(fields.data(), FIELDS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ExternalIntegrationOverview> fetchAllOverview(Long currentUserId, Long baseProfileId) {
        dsl.setLocal(SESSION_USER_ID_KEY, DSL.val(currentUserId)).execute();
        var existingExternalProfileId = DSL.field(dsl.select(EXTERNAL_PROFILE.ID)
                .from(EXTERNAL_PROFILE)
                .where(EXTERNAL_PROFILE.BASE_PROFILE_ID.eq(baseProfileId)
                        .and(EXTERNAL_PROFILE.EXTERNAL_INTEGRATION_ID.eq(EXTERNAL_INTEGRATION.ID))));
        return dsl.select(
                        EXTERNAL_INTEGRATION.ID,
                        EXTERNAL_INTEGRATION.NAME,
                        EXTERNAL_INTEGRATION.LOGO,
                        existingExternalProfileId)
                .from(EXTERNAL_INTEGRATION)
                .orderBy(EXTERNAL_INTEGRATION.ID)
                .fetch(r -> new ExternalIntegrationOverview(
                        new ExternalIntegration(
                                r.get(EXTERNAL_INTEGRATION.ID),
                                r.get(EXTERNAL_INTEGRATION.NAME),
                                null,
                                imageConverter.convert(r.get(EXTERNAL_INTEGRATION.LOGO)).orElse(null),
                                null),
                        r.get(existingExternalProfileId)
                ));
    }

    public ExternalIntegration create(Long newId, String integrationName,
                                      byte[] integrationLogo,
                                      String token,
                                      Map<String, UserField> newFields) {
        var externalIntegrationRecord = dsl.newRecord(EXTERNAL_INTEGRATION);
        externalIntegrationRecord.setId(newId);
        externalIntegrationRecord.setName(integrationName);
        externalIntegrationRecord.setLogo(integrationLogo);
        externalIntegrationRecord.setToken(token);
        externalIntegrationRecord.setProfileTemplate(serializeFields(newFields));
        externalIntegrationRecord.store();
        return new ExternalIntegration(externalIntegrationRecord.getId(),
                externalIntegrationRecord.getName(),
                externalIntegrationRecord.getToken(),
                null,
                newFields);
    }

    private JSONB serializeFields(Map<String, UserField> newFields) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(newFields));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ExternalIntegration update(Long id, String integrationName, byte[] integrationLogo, Map<String, UserField> newFields) {
        var update = dsl.update(EXTERNAL_INTEGRATION)
                .set(EXTERNAL_INTEGRATION.NAME, integrationName)
                .set(EXTERNAL_INTEGRATION.PROFILE_TEMPLATE, serializeFields(newFields));
        if (integrationLogo != null) {
            update = update.set(EXTERNAL_INTEGRATION.LOGO, integrationLogo);
        }
        update
                .where(EXTERNAL_INTEGRATION.ID.eq(id))
                .execute();
        return new ExternalIntegration(id,
                integrationName,
                null,
                null,
                newFields);
    }

    public void updateToken(Long id, String token) {
        dsl.update(EXTERNAL_INTEGRATION)
                .set(EXTERNAL_INTEGRATION.TOKEN, token)
                .where(EXTERNAL_INTEGRATION.ID.eq(id))
                .execute();
    }

    public void delete(Long id) {
        dsl.deleteFrom(EXTERNAL_INTEGRATION)
                .where(EXTERNAL_INTEGRATION.ID.eq(id))
                .execute();
    }
}
