package org.ivanvyazmitinov.selva.model;

import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * {@link ExternalIntegration} without logo bytes
 */
public record ExternalIntegrationLite(Long id,
                                      String name,
                                      @Nullable String token,
                                      @Nullable Map<String, UserField> profileTemplate) {
}
