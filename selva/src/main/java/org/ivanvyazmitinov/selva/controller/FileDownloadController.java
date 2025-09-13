package org.ivanvyazmitinov.selva.controller;


import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.transaction.Transactional;
import org.ivanvyazmitinov.selva.service.FileDownloadService;

import static io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM;

@Controller("file")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class FileDownloadController {
    private final FileDownloadService fileDownloadService;

    public FileDownloadController(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }

    @Get(value = "/{token}", produces = APPLICATION_OCTET_STREAM)
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<StreamedFile> downloadFile(String token) {
        return fileDownloadService.loadFile(token)
                .map(body -> HttpResponse.ok(body).header("Cache-Control", "no-cache, no-store"))
                .orElse(HttpResponse.notFound());
    }
}
