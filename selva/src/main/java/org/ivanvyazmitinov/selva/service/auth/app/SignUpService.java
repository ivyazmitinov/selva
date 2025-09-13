package org.ivanvyazmitinov.selva.service.auth.app;

import jakarta.inject.Singleton;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ivanvyazmitinov.selva.model.SignUpForm;
import org.ivanvyazmitinov.selva.repository.UserRepository;

@Singleton
public class SignUpService {

    private static final Log log = LogFactory.getLog(SignUpService.class);
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public SignUpService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public long signup(SignUpForm signUpForm) {
        log.debug("Creating user " + signUpForm.username());
        return userRepository.createUser(signUpForm.username(), passwordEncoder.encode(signUpForm.password()));
    }
}
