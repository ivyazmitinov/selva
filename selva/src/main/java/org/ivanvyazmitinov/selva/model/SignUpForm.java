package org.ivanvyazmitinov.selva.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import org.ivanvyazmitinov.selva.model.constraint.PasswordsMatch;

@PasswordsMatch
@Serdeable
public record SignUpForm(@NotBlank String username,
                         @NotBlank String password,
                         @NotBlank String repeatPassword) {
}
