package org.ivanvyazmitinov.selva.repository;

import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.enums.Role;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.tables.pojos.User;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.Optional;

import static org.ivanvyazmitinov.selva.repository.RepositorySessionConstants.SESSION_USER_ID_KEY;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.USER;

@Singleton
public class UserRepository {

    private final DSLContext dslContext;

    public UserRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public long createUser(String username, String password) {
        var userRecord = dslContext.newRecord(USER);
        userRecord.setUsername(username);
        userRecord.setPassword(password);
        userRecord.setRole(Role.USER);
        userRecord.store();
        return userRecord.getId();
    }

    public Optional<User> fetchUser(String username) {
        return dslContext.selectFrom(USER)
                .where(USER.USERNAME.eq(username))
                .fetchOptionalInto(User.class);
    }


    public void markRegistrationFinished(Long id) {
        dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(id)).execute();
        dslContext.update(USER)
                .set(USER.FINISHED_REGISTRATION, true)
                .where(USER.ID.eq(id))
                .execute();
    }

    public Optional<User> fetchUser(Long id) {
        dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(id)).execute();
        return dslContext.selectFrom(USER)
                .where(USER.ID.eq(id))
                .fetchOptionalInto(User.class);
    }

    public void delete(Long userId) {
        dslContext.setLocal(SESSION_USER_ID_KEY, DSL.val(userId)).execute();
        dslContext.deleteFrom(USER)
                .where(USER.ID.eq(userId))
                .execute();
    }
}
