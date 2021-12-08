package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoredFileService {

    static final String MEMBER_ASSERTION_STATS_FILE_TYPE = "assertion-stats";

    static final String ASSERTIONS_CSV_FILE_TYPE = "assertions-csv";
    
    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    public File storeMemberAssertionStatsFile(String content) throws IOException {
        Instant now = Instant.now();
        File outputFile = writeMemberAssertionStatsFile(content);
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(now);
        storedFile.setRemovalDate(now.plus(applicationProperties.getStoredFileLifespan(), ChronoUnit.DAYS));
        storedFile.setOwnerId(StoredFile.DEFAULT_SYSTEM_OWNER_ID);
        storedFile.setFileType(MEMBER_ASSERTION_STATS_FILE_TYPE);
        storedFile.setDateProcessed(now);
        storedFileRepository.save(storedFile);
        return outputFile;
    }
    
    public void storeAssertionsCsvFile(InputStream inputStream, AssertionServiceUser user) throws IOException {
        File outputFile = writeCsvUploadFile(inputStream);
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(applicationProperties.getStoredFileLifespan(), ChronoUnit.DAYS));
        storedFile.setOwnerId(user.getId());
        storedFile.setFileType(ASSERTIONS_CSV_FILE_TYPE);
        storedFileRepository.save(storedFile);
    }

    public List<StoredFile> getUnprocessedStoredFilesByType(String type) {
        return storedFileRepository.findUnprocessedByType(ASSERTIONS_CSV_FILE_TYPE);
    }
    
    public void markAsProcessed(StoredFile storedFile) {
        storedFile.setDateProcessed(Instant.now());
        storedFileRepository.save(storedFile);
    }
    
    private File writeMemberAssertionStatsFile(String content) throws IOException {
        createDir(applicationProperties.getMemberAssertionStatsDirectory());
        File outputFile = File.createTempFile(MEMBER_ASSERTION_STATS_FILE_TYPE, ".csv", new File(applicationProperties.getMemberAssertionStatsDirectory()));
        FileWriter writer = new FileWriter(outputFile);
        writer.write(content);
        writer.close();
        return outputFile;
    }
    
    private File writeCsvUploadFile(InputStream inputStream) throws IOException {
        createDir(applicationProperties.getAssertionsCsvUploadDirectory());
        File outputFile = File.createTempFile(ASSERTIONS_CSV_FILE_TYPE, ".csv", new File(applicationProperties.getAssertionsCsvUploadDirectory()));
        OutputStream outputStream = new FileOutputStream(outputFile);
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return outputFile;
    }
    
    private void createDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
               throw new RuntimeException("Failed to create directory " + path);
            }
        }
    }

}
