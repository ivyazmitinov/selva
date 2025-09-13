package org.ivanvyazmitinov.selva.service.utils;

import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.FormFieldDefault;
import org.ivanvyazmitinov.selva.model.FormFieldFile;
import org.ivanvyazmitinov.selva.model.FormFieldValue;

@Singleton
public class CompletedPartUnpacker {

    public FormFieldValue unpackCompletedPart(CompletedPart completedPart) {
        try {
            if (completedPart instanceof CompletedFileUpload completedFileUpload) {
                return new FormFieldFile(completedFileUpload.getFilename(), completedFileUpload.getBytes());
            } else {
                return new FormFieldDefault(completedPart.getBytes());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
