package org.ivanvyazmitinov.selva.model;

public record ExternalProfileEditData(ExternalIntegration integration,
                                      BaseProfile baseProfile,
                                      ExternalProfile externalProfile) {
}
