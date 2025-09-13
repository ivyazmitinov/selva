package org.ivanvyazmitinov.selva.repository.utils;

import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.apache.tika.Tika;
import org.ivanvyazmitinov.selva.model.Image;

import java.util.Optional;

@Singleton
public class ImageConverter {
    private final Tika tika = new Tika();

    public Optional<Image> convert(@Nullable byte[] image) {
        return Optional.ofNullable(image)
                .map(bytes -> new Image(bytes, tika.detect(bytes)));
    }
}
