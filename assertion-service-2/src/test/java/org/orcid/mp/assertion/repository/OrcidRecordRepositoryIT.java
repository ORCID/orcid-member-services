package org.orcid.mp.assertion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.mp.assertion.AssertionServiceApplication;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Integration test for {@link OrcidRecordRepository#findAllToInvite(String)} and
 * {@link OrcidRecordRepository#findByMemberId(String, org.springframework.data.domain.Pageable)}.
 *
 */
@SpringBootTest(classes = {AssertionServiceApplication.class})
public class OrcidRecordRepositoryIT {

    private static final String COLLECTION = "orcid_record";

    private static final String MEMBER = "60a1b2c3d4e5f60718293a4b";
    private static final String OTHER_MEMBER = "70b2c3d4e5f6071829304a5c";

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void setUp() {
        orcidRecordRepository.deleteAll();

        // --- Applicable to MEMBER: every one of these must be returned ---

        // Fresh placeholder — the ONLY shape the old equality query could match.
        insertRecord("placeholder@test.org", token(MEMBER));

        // User granted access: token carries a real token_id (old query dropped this).
        insertRecord("granted@test.org", token(MEMBER).append("token_id", "id-token-granted"));

        // User denied access: token carries denied_date (old query dropped this).
        insertRecord("denied@test.org", token(MEMBER).append("denied_date", new Date()));

        // User granted then revoked: token_id + revoked_date (old query dropped this).
        insertRecord("revoked@test.org",
                token(MEMBER).append("token_id", "id-token-revoked").append("revoked_date", new Date()));

        // Legacy token carrying salesforce_id BEFORE member_id (old query dropped this:
        insertRecord("salesforce@test.org",
                new Document("salesforce_id", "0012i00000aQxlxAAC").append("member_id", MEMBER));

        // Multiple tokens, only the second matches MEMBER — must still be returned.
        insertRecord("multi@test.org",
                token(OTHER_MEMBER).append("token_id", "other"),
                token(MEMBER).append("token_id", "mine"));

        // --- NOT applicable to MEMBER: must be excluded ---

        insertRecord("othermember@test.org", token(OTHER_MEMBER));
        insertRecordWithTokens("emptytokens@test.org", Arrays.asList());
        insertRecordWithTokens("nulltokens@test.org", null);
    }

    @Test
    public void findAllToInvite_returnsOnlyUngrantedPlaceholders() {
        assertThat(emailsOf(orcidRecordRepository.findAllToInvite(MEMBER)))
                .containsExactlyInAnyOrder(
                        "placeholder@test.org",
                        "salesforce@test.org");
    }

    @Test
    public void findAllToInvite_excludesGrantedDeniedAndRevoked() {
        Set<String> emails = emailsOf(orcidRecordRepository.findAllToInvite(MEMBER));
        assertThat(emails).doesNotContain(
                "granted@test.org",   // token_id present
                "denied@test.org",    // denied_date present
                "revoked@test.org",   // token_id + revoked_date present
                "multi@test.org");    // the MEMBER token is granted (token_id present)
    }

    @Test
    public void findAllToInvite_excludesRecordsWithNoMatchingToken() {
        Set<String> emails = emailsOf(orcidRecordRepository.findAllToInvite(MEMBER));
        assertThat(emails).doesNotContain(
                "othermember@test.org",
                "emptytokens@test.org",
                "nulltokens@test.org");
    }

    @Test
    public void findByMemberId_pagedQuery_returnsEveryRecordWithATokenForTheMember() {
        Page<OrcidRecord> page = orcidRecordRepository.findByMemberId(MEMBER, PageRequest.of(0, 50));
        assertThat(emailsOf(page.getContent())).containsExactlyInAnyOrder(
                "placeholder@test.org",
                "granted@test.org",
                "denied@test.org",
                "revoked@test.org",
                "salesforce@test.org",
                "multi@test.org");
    }

    private static Document token(String memberId) {
        return new Document("member_id", memberId);
    }

    private void insertRecord(String email, Document... tokens) {
        insertRecordWithTokens(email, Arrays.asList(tokens));
    }

    private void insertRecordWithTokens(String email, List<Document> tokens) {
        Document doc = new Document("email", email)
                .append("tokens", tokens)
                .append("created", Date.from(Instant.now()))
                .append("modified", Date.from(Instant.now()));
        mongoTemplate.getCollection(COLLECTION).insertOne(doc);
    }

    private static Set<String> emailsOf(List<OrcidRecord> records) {
        return records.stream().map(OrcidRecord::getEmail).collect(Collectors.toSet());
    }
}
