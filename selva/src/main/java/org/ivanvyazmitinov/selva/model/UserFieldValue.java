package org.ivanvyazmitinov.selva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserFieldRawValue.class, name = "raw"),
        @JsonSubTypes.Type(value = UserFieldBaseProfileValueReference.class, name = "reference")
})
public interface UserFieldValue {
}
