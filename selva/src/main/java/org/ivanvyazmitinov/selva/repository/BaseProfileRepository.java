package org.ivanvyazmitinov.selva.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.BaseProfile;
import org.ivanvyazmitinov.selva.model.UserField;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.tables.records.BaseProfileRecord;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;

import java.util.Map;
import java.util.Optional;

import static org.ivanvyazmitinov.selva.repository.RepositorySessionConstants.SESSION_USER_ID_KEY;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.BASE_PROFILE;

@Singleton
public class BaseProfileRepository {
    public static final TypeReference<Map<String, UserField>> FIELDS_TYPE_REFERENCE = new TypeReference<>() {
    };
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public BaseProfileRepository(DSLContext dslContext,
                                 ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    public long create(long userId, Map<String, UserField> userFields) {
        try {
            dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
            var baseProfileRecord = dslContext.newRecord(BASE_PROFILE);
            baseProfileRecord.setUserId(userId);
            baseProfileRecord.setFields(JSONB.valueOf(objectMapper.writeValueAsString(userFields)));
            baseProfileRecord.store();
            return baseProfileRecord.getId();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Long> fetchProfileId(Long userId) {
        dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        return dslContext.select(BASE_PROFILE.ID)
                .from(BASE_PROFILE)
                .where(BASE_PROFILE.USER_ID.eq(userId))
                .fetchOptionalInto(Long.class);
    }

    public Optional<BaseProfile> fetchProfile(Long id, Long userId) {
        dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        return dslContext.selectFrom(BASE_PROFILE)
                .where(BASE_PROFILE.ID.eq(id))
                .fetchOptionalInto(BaseProfileRecord.class)
                .map(r -> new BaseProfile(r.getId(), parseFields(r.getFields())));
    }

    public void updateProfileFields(Long id, Long userId, Map<String, UserField> userFields) {
        try {
            dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
            dslContext.update(BASE_PROFILE)
                    .set(BASE_PROFILE.FIELDS, JSONB.valueOf(objectMapper.writeValueAsString(userFields)))
                    .where(BASE_PROFILE.ID.eq(id))
                    .execute();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, UserField> parseFields(JSONB f) {
        try {
            return objectMapper.readValue(f.data(), FIELDS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
