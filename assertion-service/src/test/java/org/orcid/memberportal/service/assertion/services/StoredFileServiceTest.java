package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.StoredFileRepository;
import org.springframework.test.util.ReflectionTestUtils;

class StoredFileServiceTest {

    @InjectMocks
    private StoredFileService storedFileService;

    @Mock
    private StoredFileRepository storedFileRepository;

    @Captor
    private ArgumentCaptor<StoredFile> storedFileCaptor;

    private File storedFilesDir;

    private File memberAssertionStatsDir;

    private File assertionUploadsDir;

    private ApplicationProperties properties;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        
        storedFilesDir = File.createTempFile("test-dir", "");
        if (!storedFilesDir.delete()) {
            throw new RuntimeException("Test couldn't delete file " + storedFilesDir.getAbsolutePath());
        }
        if (!storedFilesDir.mkdir()) {
            throw new RuntimeException("Test couldn't create dir " + storedFilesDir.getAbsolutePath());
        }
        
        memberAssertionStatsDir = new File(storedFilesDir, "stats");
        if (!memberAssertionStatsDir.mkdir()) {
            throw new RuntimeException("Test couldn't create dir " + memberAssertionStatsDir.getAbsolutePath());
        }
        
        assertionUploadsDir = new File(storedFilesDir, "uploads");
        if (!assertionUploadsDir.mkdir()) {
            throw new RuntimeException("Test couldn't create dir " + assertionUploadsDir.getAbsolutePath());
        }
        
        properties = new ApplicationProperties();
        properties.setMemberAssertionStatsDirectory(memberAssertionStatsDir.getAbsolutePath());
        properties.setAssertionsCsvUploadDirectory(assertionUploadsDir.getAbsolutePath());
        properties.setStoredFileLifespan(7);
        
        ReflectionTestUtils.setField(storedFileService, "applicationProperties", properties);
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(storedFilesDir);
    }

    @Test
    void testStoreMemberAssertionStatsFile() throws IOException {
        File file = storedFileService.storeMemberAssertionStatsFile("some content");
        assertThat(file.getParent()).isEqualTo(memberAssertionStatsDir.getAbsolutePath());
        Mockito.verify(storedFileRepository).save(storedFileCaptor.capture());
        StoredFile saved = storedFileCaptor.getValue();
        assertThat(saved.getFileLocation()).isNotNull();
        assertThat(saved.getFileLocation()).startsWith(memberAssertionStatsDir.getAbsolutePath());
        assertThat(saved.getFileLocation()).endsWith(".csv");
        assertThat(saved.getFileType()).isNotNull();
        assertThat(saved.getFileType()).isEqualTo(StoredFileService.MEMBER_ASSERTION_STATS_FILE_TYPE);
        assertThat(saved.getDateWritten()).isNotNull();
        assertThat(saved.getRemovalDate()).isNotNull();
        assertThat(saved.getRemovalDate().minus(7, ChronoUnit.DAYS)).isEqualTo(saved.getDateWritten());
    }
    
    @Test
    void testStoreAssertionsCsvFile() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("some content".getBytes());
        storedFileService.storeAssertionsCsvFile(inputStream, getUser());
        Mockito.verify(storedFileRepository).save(storedFileCaptor.capture());
        StoredFile saved = storedFileCaptor.getValue();
        assertThat(saved.getFileLocation()).isNotNull();
        assertThat(saved.getFileLocation()).startsWith(storedFilesDir.getAbsolutePath());
        assertThat(saved.getFileLocation()).endsWith(".csv");
        assertThat(saved.getFileType()).isNotNull();
        assertThat(saved.getFileType()).isEqualTo(StoredFileService.ASSERTIONS_CSV_FILE_TYPE);
        assertThat(saved.getDateWritten()).isNotNull();
        assertThat(saved.getRemovalDate()).isNotNull();
        assertThat(saved.getRemovalDate().minus(7, ChronoUnit.DAYS)).isEqualTo(saved.getDateWritten());
    }
    
    @Test
    void testGetUnprocessedStoredFilesByType() {
        storedFileService.getUnprocessedStoredFilesByType(StoredFileService.ASSERTIONS_CSV_FILE_TYPE);
        Mockito.verify(storedFileRepository).findUnprocessedByType(Mockito.eq(StoredFileService.ASSERTIONS_CSV_FILE_TYPE));
    }
    
    @Test
    void testMarkAsProcessed() {
        StoredFile storedFile = new StoredFile();
        storedFileService.markAsProcessed(storedFile);
        Mockito.verify(storedFileRepository).save(storedFileCaptor.capture());
        StoredFile saved = storedFileCaptor.getValue();
        assertThat(saved.getDateProcessed()).isNotNull();
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setId("some id");
        return user;
    }

}
