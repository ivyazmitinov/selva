package org.ivanvyazmitinov.selva.model;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Map;

@Serdeable
public record ExternalApiUserProfile(String integrationName, Map<String, ExternalApiUserField> fields) {
}
