package org.ivanvyazmitinov.selva.model;

import java.util.Map;

public record BaseProfile(Long id, Map<String, UserField> userFields) {
}
