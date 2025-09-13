package org.ivanvyazmitinov.selva.service.auth.api;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.service.ExternalIntegrationService;
import org.ivanvyazmitinov.selva.service.auth.AuthConstants;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Singleton
public class ApiTokenFetcher implements AuthenticationFetcher<HttpRequest<?>> {
    private final ExternalIntegrationService externalIntegrationService;

    public ApiTokenFetcher(ExternalIntegrationService externalIntegrationService) {
        this.externalIntegrationService = externalIntegrationService;
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        return Mono.justOrEmpty(request.getHeaders().findFirst("Authorization"))
                .filter(header -> header.startsWith("Bearer"))
                .map(header -> header.substring(7))
                .flatMap(token -> processToken(token));
    }

    private Mono<@NonNull Authentication> processToken(String token) {
        var integrationId = Long.valueOf(token.split("__")[0]);
        return Mono.justOrEmpty(externalIntegrationService.findByToken(integrationId, token))
                .map(e -> Authentication.build(
                        token,
                        List.of(AuthConstants.API_INTEGRATION_ROLE_NAME),
                        Map.of(AuthConstants.INTEGRATION_ID, integrationId)));
    }
}
