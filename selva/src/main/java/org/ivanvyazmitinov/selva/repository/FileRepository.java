package org.ivanvyazmitinov.selva.repository;

import jakarta.inject.Singleton;
import org.ivanvyazmitinov.selva.model.FormFieldFile;
import org.jooq.DSLContext;

import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.FILE;

@Singleton
public class FileRepository {
    private final DSLContext dslContext;

    public FileRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public long create(String filename, byte[] fieldValueRaw) {
        var fileRecord = dslContext.newRecord(FILE);
        fileRecord.setContent(fieldValueRaw);
        fileRecord.setFileName(filename);
        fileRecord.store();
        return fileRecord.getId();
    }

    public FormFieldFile findById(Long fileId) {
        return dslContext.selectFrom(FILE)
                .where(FILE.ID.eq(fileId))
                .fetchOne(r -> new FormFieldFile(r.get(FILE.FILE_NAME), r.get(FILE.CONTENT)));
    }
}
