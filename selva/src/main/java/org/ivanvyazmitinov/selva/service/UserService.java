package org.ivanvyazmitinov.selva.service;

import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.repository.UserRepository;
import org.ivanvyazmitinov.selva.repository.jooq.generated.selva.tables.pojos.User;

import java.util.Optional;

@Singleton
public class UserService {

    private final UserRepository userRepository;

    public UserService( UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void markRegistrationFinished(Long id) {
        userRepository.markRegistrationFinished(id);
    }

    public Optional<User> fetch(Long id) {
        return userRepository.fetchUser(id);
    }

    public void delete(Long userId) {
        userRepository.delete(userId);
    }
}
