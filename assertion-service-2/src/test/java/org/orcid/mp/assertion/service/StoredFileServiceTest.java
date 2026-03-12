package org.orcid.mp.assertion.service;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.orcid.mp.assertion.domain.StoredFile;
import org.orcid.mp.assertion.domain.User;
import org.orcid.mp.assertion.repository.StoredFileRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

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

        ReflectionTestUtils.setField(storedFileService, "memberAssertionStatsDirectory", memberAssertionStatsDir.getAbsolutePath());
        ReflectionTestUtils.setField(storedFileService, "assertionsCsvUploadDirectory", assertionUploadsDir.getAbsolutePath());
        ReflectionTestUtils.setField(storedFileService, "storedFileLifespan", 7);
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
        storedFileService.storeAssertionsCsvFile(inputStream, "filename", getUser());
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
        assertThat(saved.getOriginalFilename()).isEqualTo("filename");
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

    private User getUser() {
        User user = new User();
        user.setId("some id");
        return user;
    }

}
