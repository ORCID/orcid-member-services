package org.orcid.web.rest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.client.UserSettingsClient;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.repository.AssertionsRepository;
import org.orcid.repository.OrcidRecordRepository;
import org.orcid.security.AuthoritiesConstants;
import org.orcid.security.SecurityUtils;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class AssertionServicesResource {
    private static final String ENTITY_NAME = "affiliation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private UserSettingsClient userSettingsClient;

    @Autowired
    private AssertionsRepository assertionsRepository;

    @Autowired
    private OrcidRecordRepository orcidRecordRepository;

    private String getAuthenticatedUser() {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BadRequestAlertException("User is not logged in", "login", "null");
        }

        String loggedInUser = SecurityUtils.getCurrentUserLogin().get();

        if (!SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
            throw new BadRequestAlertException("User does not have the required scope 'AuthoritiesConstants.ASSERTION_SERVICE_ENABLED'", "login", loggedInUser);
        }

        return loggedInUser;
    }

    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws BadRequestAlertException, JSONException {
        String loggedInUserId = getAuthenticatedUser();

        Page<Assertion> affiliations = assertionsRepository.findByOwnerId(loggedInUserId, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }

    @GetMapping("/assertion/{id}")
    public ResponseEntity<String> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        // TODO
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }

    @PutMapping("/assertion")
    public ResponseEntity<String> updateAssertion(@RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        // TODO
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }

    @PostMapping("/assertion")
    public ResponseEntity<Assertion> createAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, JSONException, URISyntaxException {
        validateAssertion(assertion);
        String loggedInUser = getAuthenticatedUser();
        Instant now = Instant.now();

        assertion.setOwnerId(loggedInUser);
        assertion.setCreated(now);
        assertion.setModified(now);

        String email = assertion.getEmail();

        Optional<OrcidRecord> optionalRecord = orcidRecordRepository.findOneByEmail(email);
        if (!optionalRecord.isPresent()) {
            createOrcidRecord(email, loggedInUser, now);
        }

        assertion = assertionsRepository.save(assertion);

        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, assertion.getId())).body(assertion);
    }

    @PostMapping("/assertions")
    public ResponseEntity<String> uploadAssertions(@RequestParam("file") MultipartFile file) {
        String loggedInUser = getAuthenticatedUser();

        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            List<Assertion> existingAssertions = assertionsRepository.findAllByOwnerId(loggedInUser);
            List<Assertion> affiliationsToAdd = new ArrayList<Assertion>();
            Set<String> usersToAdd = new HashSet<String>();
            // Validate affiliations
            for (CSVRecord record : elements) {
                try {
                    Assertion assertion = parseLine(record);
                    // Throw exception if found a duplicate
                    Assertion existingAssertion = getExistingAssertion(assertion, existingAssertions);
                    // If the same affiliation exists, and, the put code for it
                    // exists, check if it is an updated org
                    if (existingAssertion != null) {
                        // If something is updated in the aff, update the
                        // existing one in the DB
                        if (!StringUtils.isBlank(existingAssertion.getPutCode()) && isUpdated(assertion, existingAssertion)) {
                            copyFieldsToUpdate(aff, existingAff);
                            existingAff.setUpdated(true);
                            affiliationService.save(existingAff);
                        }
                    } else {
                        aff.setAdminId(adminUser.getId());
                        // Create the userInfo if needed
                        if (!usersToAdd.contains(aff.getEmail())) {
                            usersToAdd.add(aff.getEmail());
                        }
                        affiliationsToAdd.add(aff);
                        existingAffiliations.add(aff);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JSONObject error = new JSONObject();
                    error.put("index", record.getRecordNumber());
                    error.put("message", e.getMessage());
                    errors.put(error);
                }
            }

            if (errors.length() == 0) {
                // Create affiliations
                for (Affiliation aff : affiliationsToAdd) {
                    // Create the affiliation
                    aff = affiliationService.insert(aff);
                }

                // Create users
                for (String userEmail : usersToAdd) {
                    if (!userInfoRepository.findOneByEmail(userEmail).isPresent()) {
                        log.info("Creating UserInfo for email {}", usersToAdd);
                        createUser(userEmail, adminUser);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(errors.toString());
    }

    @DeleteMapping("/assertion/{id}")
    public ResponseEntity<String> deleteAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }

    private void createOrcidRecord(String email, String ownerId, Instant now) {
        OrcidRecord or = new OrcidRecord();
        or.setEmail(email);
        or.setOwnerId(ownerId);
        or.setCreated(now);
        or.setModified(now);
        orcidRecordRepository.insert(or);
    }

    private void validateAssertion(Assertion assertion) {
        if (StringUtils.isBlank(assertion.getEmail())) {
            throw new IllegalArgumentException("email must not be null");
        }

        if (assertion.getAffiliationSection() == null) {
            throw new IllegalArgumentException("affiliation-section must not be null");
        }
        if (StringUtils.isBlank(assertion.getOrgName())) {
            throw new IllegalArgumentException("org-name must not be null");
        }
        if (StringUtils.isBlank(assertion.getOrgCountry())) {
            throw new IllegalArgumentException("org-country must not be null");
        }
        if (StringUtils.isBlank(assertion.getOrgCity())) {
            throw new IllegalArgumentException("org-city must not be null");
        }
        if (StringUtils.isBlank(assertion.getDisambiguatedOrgId())) {
            throw new IllegalArgumentException("disambiguated-organization-identifier must not be null");
        }
        if (StringUtils.isBlank(assertion.getDisambiguationSource())) {
            throw new IllegalArgumentException("disambiguation-source must not be null");
        }
    }

    private Assertion parseLine(CSVRecord line) {
        Assertion a = new Assertion();
        if (StringUtils.isBlank(line.get("email"))) {
            throw new IllegalArgumentException("email must not be null");
        }
        a.setEmail(line.get("email"));

        if (StringUtils.isBlank(line.get("affiliation-section"))) {
            throw new IllegalArgumentException("affiliation-section must not be null");
        }
        a.setAffiliationSection(AffiliationSection.valueOf(line.get("affiliation-section").toUpperCase()));
        a.setDepartmentName(getValueOrNull(line, "department-name"));
        a.setRoleTitle(getValueOrNull(line, "role-title"));

        // Dates follows the format yyyy-MM-dd
        String startDate = line.get("start-date");
        if (!StringUtils.isBlank(startDate)) {
            String[] startDateParts = startDate.split("-|/|\\s");
            a.setStartYear(startDateParts[0]);
            if (startDateParts.length > 1) {
                a.setStartMonth(startDateParts[1]);
            }

            if (startDateParts.length > 2) {
                a.setStartDay(startDateParts[2]);
            }
        }

        // Dates follows the format yyyy-MM-dd
        String endDate = line.get("end-date");
        if (!StringUtils.isBlank(endDate)) {
            String endDateParts[] = endDate.split("-|/|\\s");
            a.setEndYear(endDateParts[0]);
            if (endDateParts.length > 1) {
                a.setEndMonth(endDateParts[1]);
            }

            if (endDateParts.length > 2) {
                a.setEndDay(endDateParts[2]);
            }
        }
        if (StringUtils.isBlank(line.get("org-name"))) {
            throw new IllegalArgumentException("org-name must not be null");
        }
        a.setOrgName(line.get("org-name"));
        if (StringUtils.isBlank(line.get("org-country"))) {
            throw new IllegalArgumentException("org-country must not be null");
        } else {
            try {
                Iso3166Country.valueOf(line.get("org-country"));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid org-country provided: " + line.get("org-country") + " it should be one from the Iso3166Country enum");
            }
        }
        a.setOrgCountry(line.get("org-country"));
        if (StringUtils.isBlank(line.get("org-city"))) {
            throw new IllegalArgumentException("org-city must not be null");
        }
        a.setOrgCity(line.get("org-city"));
        a.setOrgRegion(line.get("org-region"));
        if (StringUtils.isBlank(line.get("disambiguated-organization-identifier"))) {
            throw new IllegalArgumentException("disambiguated-organization-identifier must not be null");
        }
        a.setDisambiguatedOrgId(line.get("disambiguated-organization-identifier"));
        if (StringUtils.isBlank(line.get("disambiguation-source"))) {
            throw new IllegalArgumentException("disambiguation-source must not be null");
        }
        a.setDisambiguationSource(getValueOrNull(line, "disambiguation-source"));
        a.setExternalId(getValueOrNull(line, "external-id"));
        a.setExternalIdType(getValueOrNull(line, "external-id-type"));
        a.setExternalIdUrl(getValueOrNull(line, "external-id-url"));
        return a;
    }

    private String getValueOrNull(CSVRecord line, String name) {
        if (StringUtils.isBlank(line.get(name))) {
            return null;
        }
        return line.get(name);
    }

    private Assertion getExistingAssertion(Assertion a, List<Assertion> existing) {
        for (Assertion existingAAssertion : existing) {
            // If the email is the same
            if (a.getEmail().equals(existingAAssertion.getEmail())) {
                // And is the same affiliation type
                if (a.getAffiliationSection().equals(existingAAssertion.getAffiliationSection())) {
                    // And the same department name
                    if ((StringUtils.isBlank(a.getDepartmentName()) && StringUtils.isBlank(existingAAssertion.getDepartmentName()))
                            || (a.getDepartmentName().equals(existingAAssertion.getDepartmentName()))) {
                        // And the same role title
                        if ((StringUtils.isBlank(a.getRoleTitle()) && StringUtils.isBlank(existingAAssertion.getRoleTitle()))
                                || (a.getRoleTitle().equals(existingAAssertion.getRoleTitle()))) {
                            // And the same org name
                            if (a.getOrgName().equals(existingAAssertion.getOrgName())) {
                                return existingAAssertion;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isUpdated(Assertion a, Assertion existingAssertion) {
        // Check if something changed, if not, just ignore it
        // @formatter:off
        if (!equals(a.getDisambiguatedOrgId(), existingAssertion.getDisambiguatedOrgId()) 
                || !equals(a.getDisambiguationSource(), existingAssertion.getDisambiguationSource())
                || !equals(a.getEndDay(), existingAssertion.getEndDay()) 
                || !equals(a.getEndMonth(), existingAssertion.getEndMonth())
                || !equals(a.getEndYear(), existingAssertion.getEndYear()) 
                || !equals(a.getExternalId(), existingAssertion.getExternalId())
                || !equals(a.getExternalIdType(), existingAssertion.getExternalIdType()) 
                || !equals(a.getExternalIdUrl(), existingAssertion.getExternalIdUrl())
                || !equals(a.getOrgCity(), existingAssertion.getOrgCity()) 
                || !equals(a.getOrgCountry(), existingAssertion.getOrgCountry())
                || !equals(a.getOrgRegion(), existingAssertion.getOrgRegion()) 
                || !equals(a.getStartDay(), existingAssertion.getStartDay())
                || !equals(a.getStartMonth(), existingAssertion.getStartMonth()) 
                || !equals(a.getStartYear(), existingAssertion.getStartYear())) {
            return true;
        }
        // @formatter:on       
        return false;
    }

    private boolean equals(String a, String b) {
        return (a == null ? b == null : a.equals(b));
    }
}
