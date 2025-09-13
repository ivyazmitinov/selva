package org.ivanvyazmitinov.selva.service;

import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class TokenGenerator {

    public String generateToken(String payload) {
        return"%s__%s".formatted(payload, UUID.randomUUID().toString());
    }

}
