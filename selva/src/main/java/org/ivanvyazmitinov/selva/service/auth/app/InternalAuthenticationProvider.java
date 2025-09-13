package org.ivanvyazmitinov.selva.service.auth.app;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.repository.BaseProfileRepository;
import org.ivanvyazmitinov.selva.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.ivanvyazmitinov.selva.service.auth.AuthConstants.*;

@Singleton
public class InternalAuthenticationProvider<B> implements HttpRequestAuthenticationProvider<B> {

    private final UserRepository userRepository;
    private final BaseProfileRepository baseProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public InternalAuthenticationProvider(UserRepository userRepository,
                                          BaseProfileRepository baseProfileRepository,
                                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.baseProfileRepository = baseProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public @NonNull AuthenticationResponse authenticate(@Nullable HttpRequest<B> requestContext,
                                                        @NonNull AuthenticationRequest<String, String> authRequest) {
        var userName = authRequest.getIdentity();
        var user = userRepository.fetchUser(userName);
        if (user.isEmpty()) {
            return AuthenticationResponse.failure("User %s not found".formatted(userName));
        } else if (!(passwordEncoder.matches(authRequest.getSecret(), user.get().password()))) {
            return AuthenticationResponse.failure("Wrong password");
        }

        var userId = user.get().id();
        return switch (user.get().role()) {
            // Role is duplicated in the attributes to be available in view.
            // See SecurityViewModelProcessor
            case ADMIN -> AuthenticationResponse.success(userName,
                    List.of(user.get().role().getLiteral()),
                    Map.of(USER_ID, userId,
                            USER_ROLE, user.get().role().getLiteral()));
            case USER -> {
                var userProfileId = baseProfileRepository.fetchProfileId(userId);
                if (userProfileId.isEmpty()) {
                    throw new IllegalStateException("Profile for user %s is not found".formatted(userId));
                }

                yield AuthenticationResponse.success(userName,
                        List.of(user.get().role().getLiteral()),
                        Map.of(PROFILE_ID, userProfileId.get(),
                                USER_ID, userId,
                                // Role is duplicated to be available in view. See SecurityViewModelProcessor
                                USER_ROLE, user.get().role().getLiteral()));
            }
        };


    }
}
