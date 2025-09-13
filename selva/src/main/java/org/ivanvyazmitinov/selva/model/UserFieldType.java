package org.ivanvyazmitinov.selva.model;

import java.time.LocalDate;

public enum UserFieldType {
    TEXT(String.class),
    FILE(byte[].class),
    DATE(LocalDate.class),
    ;
    private final Class<?> typeClass;

    UserFieldType(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }
}
