package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.io.FileOutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoredFileService {

    private static final Logger LOG = LoggerFactory.getLogger(StoredFileService.class);

    static final String MEMBER_ASSERTION_STATS_FILE_TYPE = "assertion-stats";

    static final String ASSERTIONS_CSV_FILE_TYPE = "assertions-csv";
    
    static final String CSV_REPORT_FILE_TYPE = "csv-report";

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

    public void storeAssertionsCsvFile(InputStream inputStream, String filename, AssertionServiceUser user) throws IOException {
        File outputFile = writeCsvUploadFile(inputStream);
        StoredFile storedFile = new StoredFile();
        storedFile.setOriginalFilename(filename);
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(applicationProperties.getStoredFileLifespan(), ChronoUnit.DAYS));
        storedFile.setOwnerId(user.getId());
        storedFile.setFileType(ASSERTIONS_CSV_FILE_TYPE);
        storedFileRepository.save(storedFile);
    }
    
    public StoredFile storeCsvReportFile(String report, String originalFilename, AssertionServiceUser user) throws IOException {
        File outputFile = writeCsvReportFile(report);
        StoredFile storedFile = new StoredFile();
        storedFile.setOriginalFilename(originalFilename);
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(applicationProperties.getStoredFileLifespan(), ChronoUnit.DAYS));
        storedFile.setOwnerId(user.getId());
        storedFile.setFileType(CSV_REPORT_FILE_TYPE);
        return storedFileRepository.save(storedFile);
    }

    public List<StoredFile> getUnprocessedStoredFilesByType(String type) {
        return storedFileRepository.findUnprocessedByType(ASSERTIONS_CSV_FILE_TYPE);
    }

    public void markAsProcessed(StoredFile storedFile) {
        storedFile.setDateProcessed(Instant.now());
        storedFileRepository.save(storedFile);
    }

    private File writeCsvReportFile(String content) throws IOException {
        createDir(applicationProperties.getCsvReportsDirectory());
        return writeStringToFile(content, CSV_REPORT_FILE_TYPE, ".csv", new File(applicationProperties.getCsvReportsDirectory()));
    }
    
    private File writeMemberAssertionStatsFile(String content) throws IOException {
        createDir(applicationProperties.getMemberAssertionStatsDirectory());
        return writeStringToFile(content, MEMBER_ASSERTION_STATS_FILE_TYPE, ".csv", new File(applicationProperties.getMemberAssertionStatsDirectory()));
    }

    private File writeCsvUploadFile(InputStream inputStream) throws IOException {
        createDir(applicationProperties.getAssertionsCsvUploadDirectory());
        return writeInputStreamToFile(inputStream, ASSERTIONS_CSV_FILE_TYPE, ".csv", new File(applicationProperties.getAssertionsCsvUploadDirectory()));
    }
    
    private File writeStringToFile(String content, String fileType, String extension, File parent) throws IOException {
        File outputFile = File.createTempFile(fileType, extension, parent);
        OutputStream outputStream = new FileOutputStream(outputFile);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();
        return outputFile;
    }
    
    private File writeInputStreamToFile(InputStream inputStream, String fileType, String extension, File parent) throws IOException {
        File outputFile = File.createTempFile(fileType, extension, parent);
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

    public void removeStoredFiles() {
        List<StoredFile> oldFiles = storedFileRepository.findProcessedFiles();
        oldFiles.forEach(f -> {
            if (Instant.now().isAfter(f.getRemovalDate())) {
                removeStoredFile(f);
            }
        });
    }

    private void removeStoredFile(StoredFile f) {
        LOG.info("Removing file {} which is marked for deletion", f.getFileLocation());
        File file = new File(f.getFileLocation());
        boolean deleted = file.delete();
        if (!deleted) {
            LOG.error("Failed to delete file {}", file.getAbsolutePath());
        } else {
            LOG.info("File {} deleted", file.getAbsolutePath());
            LOG.info("Removing corresponding StoredFile record");
            storedFileRepository.delete(f);
            LOG.info("Record deleted");
        }
    }

}
