package org.ivanvyazmitinov.selva.model;

public record FormFieldFile(String fileName, byte[] content) implements FormFieldValue {
}
