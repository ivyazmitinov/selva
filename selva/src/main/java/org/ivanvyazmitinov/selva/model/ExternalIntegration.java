package org.ivanvyazmitinov.selva.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public record ExternalIntegration(@Nonnull Long id,
                                  @Nonnull String name,
                                  @Nullable String token,
                                  @Nullable Image logo,
                                  @Nullable Map<String, UserField> profileTemplate) {
}
