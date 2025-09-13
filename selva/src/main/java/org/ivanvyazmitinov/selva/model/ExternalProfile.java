package org.ivanvyazmitinov.selva.model;

import jakarta.annotation.Nonnull;

import java.util.Map;

public record ExternalProfile(@Nonnull Long id,
                              @Nonnull Long baseProfileId,
                              @Nonnull Long externalIntegrationId,
                              boolean isPublic,
                              @Nonnull Map<String, UserField> fields) {
}
