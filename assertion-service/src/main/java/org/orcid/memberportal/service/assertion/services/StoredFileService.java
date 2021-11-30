package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoredFileService {

    static final String MEMBER_ASSERTION_STATS_FILE_TYPE = "assertion-stats";

    @Autowired
    private StoredFileRepository storedFileRespository;

    @Autowired
    private ApplicationProperties applicationProperties;

    public File storeMemberAssertionStatsFile(String content) throws IOException {
        File outputFile = writeFile(content, MEMBER_ASSERTION_STATS_FILE_TYPE, applicationProperties.getMemberAssertionStatsDirectory(), ".csv");
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation(outputFile.getAbsolutePath());
        storedFile.setDateWritten(Instant.now());
        storedFile.setRemovalDate(storedFile.getDateWritten().plus(applicationProperties.getStoredFileLifespan(), ChronoUnit.DAYS));
        storedFile.setFileType(MEMBER_ASSERTION_STATS_FILE_TYPE);
        storedFileRespository.save(storedFile);
        return outputFile;
    }

    private File writeFile(String content, String memberAssertionStatsFileType, String memberAssertionStatsFileDirectory, String extension) throws IOException {
        File parentDir = new File(memberAssertionStatsFileDirectory);
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdir();
            if (!created) {
               throw new RuntimeException("Failed to create directory " + memberAssertionStatsFileDirectory);
            }
        }
        
        File outputFile = File.createTempFile(memberAssertionStatsFileType, extension, new File(memberAssertionStatsFileDirectory));
        FileWriter writer = new FileWriter(outputFile);
        writer.write(content);
        writer.close();
        return outputFile;
    }

}
