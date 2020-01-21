package org.orcid.user.web.rest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import io.micrometer.core.instrument.util.StringUtils;

/**
 * REST controller for managing {@link org.orcid.user.domain.MemberSettings}.
 */
@RestController
@RequestMapping("/settings/api")
public class MemberSettingsResource {

    private final Logger log = LoggerFactory.getLogger(MemberSettingsResource.class);

    private static final String ENTITY_NAME = "userSettingsServiceMemberSettings";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserSettingsRepository userSettingsRepository;

    private final MemberSettingsRepository memberSettingsRepository;

    public MemberSettingsResource(MemberSettingsRepository memberSettingsRepository, UserSettingsRepository userSettingsRepository) {
        this.memberSettingsRepository = memberSettingsRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    /**
     * {@code POST  /member-settings} : Create a new memberSettings.
     *
     * @param memberSettings
     *            the memberSettings to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with body the new memberSettings, or with status
     *         {@code 400 (Bad Request)} if the memberSettings has already an
     *         ID.
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect.
     */
    @PostMapping("/member-settings")
    public ResponseEntity<MemberSettings> createMemberSettings(@Valid @RequestBody MemberSettings memberSettings) throws URISyntaxException {
        log.debug("REST request to save MemberSettings : {}", memberSettings);
        if (memberSettings.getId() != null) {
            throw new BadRequestAlertException("A new memberSettings cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Optional<MemberSettings> optional = memberSettingsRepository.findBySalesforceId(memberSettings.getSalesforceId());
        // If user doesn't exists, create it
        if(optional.isPresent()) {
            throw new BadRequestAlertException("A member settings with that salesforce id already exists", ENTITY_NAME, "idexists");
        }
        if (!validate(memberSettings)) {
            ResponseEntity.badRequest()
            .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "memberSettings.create.error", memberSettings.getError()));                    
        }
        MemberSettings result = memberSettingsRepository.save(memberSettings);
        return ResponseEntity.created(new URI("/api/member-settings/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString())).body(result);
    }

    /**
     * {@code POST  /member-settings/upload} : Create a list of member settings.
     *
     * @param file:
     *            file containing the member-settings to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with a map indicating if each user was created or not, or with
     *         status {@code 400 (Bad Request)} if the file cannot be parsed.
     * @throws Throwable
     */
    @PostMapping("/member-settings/upload")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<String> uploadMemberSettings(@RequestParam("file") MultipartFile file) throws Throwable {
        log.debug("Uploading member settings CSV");
        JSONArray errors = new JSONArray();
        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            for (CSVRecord line : elements) {
                long index = line.getRecordNumber();
                try {
                    MemberSettings memberSettings = parseLine(line);
                    // Validate for errors
                    if (!validate(memberSettings)) {
                        JSONObject error = new JSONObject();
                        error.put("index", index);
                        error.put("message", memberSettings.getError());
                        errors.put(error);
                    } else {
                        Optional<MemberSettings> optional = memberSettingsRepository.findBySalesforceId(memberSettings.getSalesforceId());
                        // If user doesn't exists, create it
                        if(!optional.isPresent()) {
                            Instant now = Instant.now();
                            memberSettings.setCreatedBy(SecurityUtils.getAuthenticatedUser());
                            memberSettings.setCreatedDate(now);
                            memberSettings.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
                            memberSettings.setLastModifiedDate(now);
                            memberSettingsRepository.save(memberSettings);
                        } else {
                            // If it exists, update it
                            MemberSettings existingMemberSettings = optional.get();
                            existingMemberSettings.setAssertionServiceEnabled(memberSettings.getAssertionServiceEnabled());
                            existingMemberSettings.setClientId(memberSettings.getClientId());
                            existingMemberSettings.setIsConsortiumLead(memberSettings.getIsConsortiumLead());
                            existingMemberSettings.setParentSalesforceId(memberSettings.getParentSalesforceId());
                            memberSettingsRepository.save(existingMemberSettings);
                        }
                    }
                } catch (Exception e) {
                    Throwable t = e.getCause();
                    JSONObject error = new JSONObject();
                    error.put("index", index);
                    if (t != null) {
                        log.error("Error on line " + index, t.getMessage());
                        error.put("message", t.getMessage());
                    } else {
                        log.error("Error on line " + index, e.getMessage());
                        error.put("message", e.getMessage());
                    }
                    errors.put(error);
                }
            }
        }
        return ResponseEntity.ok().body(errors.toString());
    }

    /**
     * {@code PUT  /member-settings} : Updates an existing memberSettings.
     *
     * @param memberSettings
     *            the memberSettings to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the updated memberSettings, or with status
     *         {@code 400 (Bad Request)} if the memberSettings is not valid, or
     *         with status {@code 500 (Internal Server Error)} if the
     *         memberSettings couldn't be updated.
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect.
     */
    @PutMapping("/member-settings")
    public ResponseEntity<MemberSettings> updateMemberSettings(@Valid @RequestBody MemberSettings memberSettings) throws URISyntaxException {
        log.debug("REST request to update MemberSettings : {}", memberSettings);
        if (memberSettings.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Optional<MemberSettings> mso = memberSettingsRepository.findById(memberSettings.getId());
        if (!mso.isPresent()) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idunavailable");
        }
        if (!validate(memberSettings)) {
            ResponseEntity.badRequest()
            .headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "memberSettings.create.error", memberSettings.getError()));                    
        }
        
        MemberSettings result = memberSettingsRepository.save(memberSettings);

        // Check if salesforceId changed
        MemberSettings existingMemberSettings = mso.get();

        if (!existingMemberSettings.getSalesforceId().equals(memberSettings.getSalesforceId())) {
            // If salesforceId changed, update each of the existing users with
            // the new salesforceId
            userSettingsRepository.findBySalesforceId(existingMemberSettings.getSalesforceId()).stream().forEach(userSettings -> {
                userSettings.setSalesforceId(memberSettings.getSalesforceId());
                userSettingsRepository.save(userSettings);
            });
        }

        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, memberSettings.getId().toString())).body(result);
    }

    /**
     * {@code GET  /member-settings} : get all the memberSettings.
     *
     * 
     * @param pageable
     *            the pagination information.
     * 
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the
     *         list of memberSettings in body.
     */
    @GetMapping("/member-settings")
    public ResponseEntity<List<MemberSettings>> getAllMemberSettings(Pageable pageable) {
        log.debug("REST request to get a page of MemberSettings");
        Page<MemberSettings> page = memberSettingsRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /member-settings/:id} : get the "id" memberSettings.
     *
     * @param id
     *            the id of the memberSettings to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the memberSettings, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/member-settings/{id}")
    public ResponseEntity<MemberSettings> getMemberSettings(@PathVariable String id) {
        log.debug("REST request to get MemberSettings : {}", id);
        Optional<MemberSettings> memberSettings = memberSettingsRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(memberSettings);
    }

    /**
     * {@code DELETE  /member-settings/:id} : delete the "id" memberSettings.
     *
     * @param id
     *            the id of the memberSettings to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/member-settings/{id}")
    public ResponseEntity<Void> deleteMemberSettings(@PathVariable String id) {
        log.debug("REST request to delete MemberSettings : {}", id);

        // Can't delete a memberSettings object if there is at least one
        // userSettings linked to it
        Optional<MemberSettings> mso = memberSettingsRepository.findById(id);
        if (!mso.isPresent()) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idunavailable");
        }

        // If there is at least one userSettings assigned to this
        // memberSettings, throw an exception
        userSettingsRepository.findBySalesforceId(id).stream().map(userSettings -> {
            throw new BadRequestAlertException("Unable to delete MemberSettings, user '" + userSettings.getLogin() + "' still use it", ENTITY_NAME, "idused");
        });

        memberSettingsRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }

    private MemberSettings parseLine(CSVRecord record) {
        MemberSettings ms = new MemberSettings();
        if (record.isSet("assertion_services_enabled")) {
            ms.setAssertionServiceEnabled(Boolean.parseBoolean(record.get("assertion_services_enabled")));
        } else {
            ms.setAssertionServiceEnabled(false);
        }
        ms.setClientId(record.get("client_id"));
        Boolean isConsortiumLead = false;
        if (record.isSet("is_consortium_lead")) {
            isConsortiumLead = Boolean.parseBoolean(record.get("is_consortium_lead"));
        }
        ms.setIsConsortiumLead(isConsortiumLead);
        ms.setSalesforceId(record.get("salesforce_id"));

        if (!isConsortiumLead) {
            ms.setParentSalesforceId(record.get("parent_salesforce_id"));
        }
        if(record.isSet("client_name")) {
            ms.setClientName(record.get("client_name"));
        }
        return ms;
    }
    
    private boolean validate(MemberSettings ms) {
        boolean isOk = true;
        if (StringUtils.isBlank(ms.getClientId())) {
            isOk = false;
            ms.setError("Client id should not be empty");
        }
        
        if (StringUtils.isBlank(ms.getSalesforceId())) {
            isOk = false;
            ms.setError("Salesforce id should not be empty");
        }
        
        if (StringUtils.isBlank(ms.getParentSalesforceId()) && !ms.getIsConsortiumLead()) {
            isOk = false;
            ms.setError("Parent salesforce id should not be empty if it is not a consortium lead");
        }
        return isOk;
    }
}
