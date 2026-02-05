package org.orcid.mp.assertion.service;

import org.apache.commons.io.IOUtils;
import org.orcid.mp.assertion.domain.StoredFile;
import org.orcid.mp.assertion.domain.User;
import org.orcid.mp.assertion.repository.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StoredFileService {

    @Value("${application.files.storedFileLifespan}")
    private int storedFileLifespan;

    @Value("${application.files.memberAssertionStatsDirectory}")
    private String memberAssertionStatsDirectory;

    @Value("${application.files.assertionsCsvUploadDirectory}")
    private String assertionsCsvUploadDirectory;

    @Value("${application.files.csvReportsDirectory}")
    private String csvReportsDirectory;

    private static final Logger LOG = LoggerFactory.getLogger(StoredFileService.class);

    static final String MEMBER_ASSERTION_STATS_FILE_TYPE = "assertion-stats";

    static final String ASSERTIONS_CSV_FILE_TYPE = "assertions-csv";

    static final String CSV_REPORT_FILE_TYPE = "csv-report";

    @Autowired
    private StoredFileRepository storedFileRepository;

    public File storeMemberAssertionStatsFile(String content) throws IOException {
        Instant now = Instant.now();
        File outputFile = writeMemberAssertionStatsFile(content);
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(now);
        storedFile.setRemovalDate(now.plus(storedFileLifespan, ChronoUnit.DAYS));
        storedFile.setOwnerId(StoredFile.DEFAULT_SYSTEM_OWNER_ID);
        storedFile.setFileType(MEMBER_ASSERTION_STATS_FILE_TYPE);
        storedFile.setDateProcessed(now);
        storedFileRepository.save(storedFile);
        return outputFile;
    }

    public List<StoredFile> getUnprocessedStoredFilesByType(String type) {
        return storedFileRepository.findUnprocessedByType(ASSERTIONS_CSV_FILE_TYPE);
    }

    public void markAsProcessed(StoredFile storedFile) {
        storedFile.setDateProcessed(Instant.now());
        storedFileRepository.save(storedFile);
    }

    public StoredFile storeCsvReportFile(String report, String originalFilename, User user) throws IOException {
        File outputFile = writeCsvReportFile(report);
        StoredFile storedFile = new StoredFile();
        storedFile.setOriginalFilename(originalFilename);
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(storedFileLifespan, ChronoUnit.DAYS));
        storedFile.setOwnerId(user.getId());
        storedFile.setFileType(CSV_REPORT_FILE_TYPE);
        return storedFileRepository.save(storedFile);
    }

    public void storeAssertionsCsvFile(InputStream inputStream, String filename, User user) throws IOException {
        File outputFile = writeCsvUploadFile(inputStream);
        StoredFile storedFile = new StoredFile();
        storedFile.setOriginalFilename(filename);
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(storedFileLifespan, ChronoUnit.DAYS));
        storedFile.setOwnerId(user.getId());
        storedFile.setFileType(ASSERTIONS_CSV_FILE_TYPE);
        storedFileRepository.save(storedFile);
    }

    private File writeMemberAssertionStatsFile(String content) throws IOException {
        createDir(memberAssertionStatsDirectory);
        return writeStringToFile(content, MEMBER_ASSERTION_STATS_FILE_TYPE, ".csv", new File(memberAssertionStatsDirectory));
    }

    private File writeCsvUploadFile(InputStream inputStream) throws IOException {
        createDir(assertionsCsvUploadDirectory);
        return writeInputStreamToFile(inputStream, ASSERTIONS_CSV_FILE_TYPE, ".csv", new File(assertionsCsvUploadDirectory));
    }

    private File writeCsvReportFile(String content) throws IOException {
        createDir(csvReportsDirectory);
        return writeStringToFile(content, CSV_REPORT_FILE_TYPE, ".csv", new File(csvReportsDirectory));
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
