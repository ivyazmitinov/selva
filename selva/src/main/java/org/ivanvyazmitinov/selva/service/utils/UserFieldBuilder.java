package org.ivanvyazmitinov.selva.service.utils;

import io.micronaut.http.multipart.CompletedPart;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.*;
import org.ivanvyazmitinov.selva.repository.ExternalProfileRepository;
import org.ivanvyazmitinov.selva.repository.FileRepository;

import java.util.Optional;

@Singleton
public class UserFieldBuilder {

    private final FileRepository fileRepository;

    public UserFieldBuilder(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public UserFieldRawValue prepareFieldValue(@Nullable FormFieldValue fieldValue) {
        return switch (fieldValue) {
            case FormFieldDefault(byte[] content) -> new UserFieldRawValue(new String(content));
            case FormFieldFile(String filename, byte[] content) -> {
                // Files are persisted separately first
                var f = fileRepository.create(filename, content);
                yield new UserFieldRawValue(f);
            }
            case null -> null;
        };
    }

}
