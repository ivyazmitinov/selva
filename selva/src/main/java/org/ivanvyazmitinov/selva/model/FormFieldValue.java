package org.ivanvyazmitinov.selva.model;

public sealed interface FormFieldValue permits FormFieldDefault, FormFieldFile {
    byte[] content();
}
