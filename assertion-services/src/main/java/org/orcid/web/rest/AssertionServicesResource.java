package org.orcid.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.InternalServerErrorException;
import javax.xml.bind.JAXBException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.security.EncryptUtil;
import org.orcid.security.JWTUtil;
import org.orcid.security.SecurityUtils;
import org.orcid.service.AssertionsService;
import org.orcid.service.OrcidRecordService;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class AssertionServicesResource {
    private final Logger log = LoggerFactory.getLogger(AssertionServicesResource.class);

    private static final String ENTITY_NAME = "affiliation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private AssertionsService assertionsService;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private JWTUtil jwtUtil;
    
    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws BadRequestAlertException, JSONException {
        log.debug("REST request to fetch assertions from user {}", SecurityUtils.getCurrentUserLogin().get());

        Page<Assertion> affiliations = assertionsService.findByOwnerId(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }

    @GetMapping("/assertion/{id}")
    public ResponseEntity<Assertion> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {        
        log.debug("REST request to fetch assertion {} from user {}", id, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok().body(assertionsService.findById(id));
    }
    
    @GetMapping("/assertion/links")
    public void generateLinks(HttpServletResponse response) throws IOException, JSONException {
        String loggedInUserId = SecurityUtils.getCurrentUserLogin().get();
        final String fileName = loggedInUserId + '_' +  System.currentTimeMillis() + "_report.csv";        
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = orcidRecordService.generateLinks();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
    }

    @PutMapping("/assertion")
    public ResponseEntity<Assertion> updateAssertion(@RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        log.debug("REST request to update assertion : {}", assertion);        
        validateAssertion(assertion);
        Assertion existingAssertion = assertionsService.updateAssertion(assertion);
        
        return ResponseEntity.ok().body(existingAssertion);
    }

    @PostMapping("/assertion")
    public ResponseEntity<Assertion> createAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, URISyntaxException {
        log.debug("REST request to create assertion : {}", assertion);        
        validateAssertion(assertion);
        assertion = assertionsService.createAssertion(assertion);

        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, assertion.getId())).body(assertion);
    }

    @PostMapping("/assertion/upload")
    public ResponseEntity<String> uploadAssertions(@RequestParam("file") MultipartFile file) {
        JSONArray errors = new JSONArray();
        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            List<Assertion> existingAssertions = assertionsService.findAllByOwnerId();
            List<Assertion> assertionsToAdd = new ArrayList<Assertion>();
            Set<String> usersToAdd = new HashSet<String>();
            // Validate affiliations
            for (CSVRecord record : elements) {
                try {
                    Assertion assertion = parseLine(record);
                    // Throw exception if found a duplicate
                    Assertion existingAssertion = getExistingAssertion(assertion, existingAssertions);
                    // If the same assertion exists, and, the put code for it
                    // exists, check if it is an updated org
                    if (existingAssertion != null) {
                        // If something is updated in the aff, update the
                        // existing one in the DB
                        if (!StringUtils.isBlank(existingAssertion.getPutCode()) && isUpdated(assertion, existingAssertion)) {
                            assertionsService.updateAssertion(existingAssertion);
                        }
                    } else {
                        // Create the userInfo if needed
                        if (!usersToAdd.contains(assertion.getEmail())) {
                            usersToAdd.add(assertion.getEmail());
                        }
                        assertionsToAdd.add(assertion);
                        existingAssertions.add(assertion);
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
                assertionsService.createAssertions(assertionsToAdd);
                orcidRecordService.createOrcidRecords(usersToAdd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(errors.toString());
    }

    @DeleteMapping("/assertion/{id}")
    public ResponseEntity<String> deleteAssertion(@PathVariable String id) throws BadRequestAlertException {
        assertionsService.deleteById(id);

        return ResponseEntity.ok().body("{\"id\":\"" + id + "\"}");
    }

    @DeleteMapping("/assertion/orcid/{id}")
    public ResponseEntity<String> deleteAssertionFromOrcid(@PathVariable String id) throws JAXBException {
        Boolean deleted = assertionsService.deleteAssertionFromOrcid(id);
        return ResponseEntity.ok().body("{\"deleted\":\"" + deleted + "\"}");
    }
    
    @PostMapping("/id-token")
    public ResponseEntity<Void> storeIdToken(@RequestBody ObjectNode json) throws ParseException {
        String state = json.get("state").asText();
        String idToken = json.has("id_token") ? json.get("id_token").asText() : null;
        Boolean denied = json.has("denied") ? json.get("denied").asBoolean() : false;
        String emailInStatus = encryptUtil.decrypt(state);

        if (!denied) {
            SignedJWT jwt = jwtUtil.getSignedJWT(idToken);
            String orcidIdInJWT = String.valueOf(jwt.getJWTClaimsSet().getClaim("sub"));

            if (!StringUtils.isBlank(emailInStatus) && !StringUtils.isBlank(orcidIdInJWT)) {
                orcidRecordService.storeIdToken(emailInStatus, idToken, orcidIdInJWT);
            } else {
                if (StringUtils.isBlank(emailInStatus)) {
                    log.warn("emailInStatus is empty in the state key: " + state);
                }

                if (StringUtils.isBlank(orcidIdInJWT)) {
                    log.warn("orcidIdInJWT is empty in the id token: " + idToken);
                }
            }
        } else {
            log.warn("User {} have denied access", emailInStatus);
            orcidRecordService.storeUserDeniedAccess(emailInStatus);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/assertion/report")    
    public void generateReport(HttpServletResponse response) throws IOException {        
        final String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new InternalServerErrorException("Current user login not found"));
        final String fileName = userLogin + '_' +  System.currentTimeMillis() + "_report.csv";        
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = assertionsService.generateAssertionsReport();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
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
        a.setDepartmentName(getMandatoryNullableValue(line, "department-name"));
        a.setRoleTitle(getMandatoryNullableValue(line, "role-title"));

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
        a.setDisambiguationSource(getMandatoryNullableValue(line, "disambiguation-source"));
        a.setExternalId(getOptionalMandatoryNullable(line, "external-id"));
        a.setExternalIdType(getOptionalMandatoryNullable(line, "external-id-type"));
        a.setExternalIdUrl(getOptionalMandatoryNullable(line, "external-id-url"));
        return a;
    }

    private String getMandatoryNullableValue(CSVRecord line, String name) {
        if (StringUtils.isBlank(line.get(name))) {
            return null;
        }
        return line.get(name);
    }
    
    private String getOptionalMandatoryNullable(CSVRecord line, String name) {
    	try {
	        if (StringUtils.isBlank(line.get(name))) {
	            return null;
	        }
	        return line.get(name);
    	} catch (IllegalArgumentException e) {
    		return null;
    	}
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
                || !equals(a.getStartYear(), existingAssertion.getStartYear())
                || !equals(a.getDepartmentName(), existingAssertion.getDepartmentName())) {
            return true;
        }
        // @formatter:on       
        return false;
    }

    private boolean equals(String a, String b) {
        return (a == null ? b == null : a.equals(b));
    }    
}
