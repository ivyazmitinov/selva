package org.ivanvyazmitinov.selva.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

@Serdeable
public record ExternalApiUserField(@Nullable Object value) {
}
