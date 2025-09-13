package org.ivanvyazmitinov.selva.model.constraint;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.ivanvyazmitinov.selva.model.SignUpForm;

@Introspected
public class PasswordMatchValidator implements ConstraintValidator<PasswordsMatch, SignUpForm> {
    @Override
    public boolean isValid(SignUpForm form, ConstraintValidatorContext context) {
        return form.password().equals(form.repeatPassword());
    }
}
