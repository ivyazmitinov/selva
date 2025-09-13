package org.ivanvyazmitinov.selva.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@Serdeable
public record ExternalApiUserProfiles(@Nullable ExternalApiUserProfile current,
                                      @Nonnull List<ExternalApiUserProfile> other) {
}
