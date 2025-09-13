package org.ivanvyazmitinov.selva.model;

public record UserField(String name,
                        Integer order,
                        UserFieldType type,
                        UserFieldValue value) {
}
