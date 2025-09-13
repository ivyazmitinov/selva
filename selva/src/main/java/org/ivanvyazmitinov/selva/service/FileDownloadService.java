package org.ivanvyazmitinov.selva.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micronaut.context.annotation.Context;
import io.micronaut.http.server.types.files.StreamedFile;
import org.ivanvyazmitinov.selva.config.AppProperties;
import org.ivanvyazmitinov.selva.model.FormFieldFile;
import org.ivanvyazmitinov.selva.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.micronaut.http.MediaType.APPLICATION_OCTET_STREAM_TYPE;

@Context
public class FileDownloadService {
    private final static Logger log = LoggerFactory.getLogger(FileDownloadService.class);
    private final Cache<String, Lock> downloadTokens = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();
    private final TokenGenerator tokenGenerator;
    private final FileRepository fileRepository;
    private final AppProperties appProperties;

    public FileDownloadService(TokenGenerator tokenGenerator,
                               FileRepository fileRepository,
                               AppProperties appProperties) {
        this.tokenGenerator = tokenGenerator;
        this.fileRepository = fileRepository;
        this.appProperties = appProperties;
    }

    public String prepareFileDownload(Long fileId) {
        var downloadToken = tokenGenerator.generateToken(fileId.toString());
        downloadTokens.put(downloadToken, new ReentrantLock());
        return "%s/file/%s".formatted(appProperties.baseUrl(), downloadToken);
    }

    public Optional<StreamedFile> loadFile(String token) {
        return Optional.ofNullable(downloadTokens.getIfPresent(token))
                .flatMap(lock -> loadFileOneTime(token, lock));
    }

    private Optional<StreamedFile> loadFileOneTime(String token, Lock lock) {
        try {
            lock.lock();
            if (downloadTokens.getIfPresent(token) == null) {
                return Optional.empty();
            }
            var fileId = Long.valueOf(token.split("__")[0]);
            var file = fileRepository.findById(fileId);
            return Optional.of(new StreamedFile(new ByteArrayInputStream(file.content()), APPLICATION_OCTET_STREAM_TYPE).attach(file.fileName()));
        } finally {
            downloadTokens.invalidate(token);
            lock.unlock();
        }
    }
}
