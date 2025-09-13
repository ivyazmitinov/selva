package org.ivanvyazmitinov.selva.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(String baseUrl) {
}
