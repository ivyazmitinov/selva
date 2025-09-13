package org.ivanvyazmitinov.selva.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ExternalIntegrationOverview(@Nonnull ExternalIntegration integration,
                                          @Nullable Long externalProfileId) {
}
