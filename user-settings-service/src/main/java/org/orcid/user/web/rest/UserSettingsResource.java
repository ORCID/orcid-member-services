package org.orcid.user.web.rest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import io.micrometer.core.instrument.util.StringUtils;
import net.logstash.logback.encoder.org.apache.commons.lang3.BooleanUtils;

/**
 * REST controller for managing
 * {@link org.orcid.user.domain.MemberServicesUser}.
 */
@RestController
@RequestMapping("/settings/api")
public class UserSettingsResource {

    private final Logger log = LoggerFactory.getLogger(UserSettingsResource.class);

    private static final String ENTITY_NAME = "userSettingsServiceMemberServicesUser";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private Oauth2ServiceClient oauth2ServiceClient;

    private final UserSettingsRepository userSettingsRepository;

    private final MemberSettingsRepository memberSettingsRepository;

    public UserSettingsResource(UserSettingsRepository userSettingsRepository, MemberSettingsRepository memberSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.memberSettingsRepository = memberSettingsRepository;
    }

    /**
     * {@code POST  /user/upload} : Create a list of users.
     *
     * @param file:
     *            file containing the users to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with a map indicating if each user was created or not, or with
     *         status {@code 400 (Bad Request)} if the file cannot be parsed.
     * @throws Throwable
     */
    @PostMapping("/user/upload")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<String> uploadUsers(@RequestParam("file") MultipartFile file) throws Throwable {
        log.debug("Uploading users settings CSV");
        JSONArray errors = new JSONArray();
        Instant now = Instant.now();
        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            for (CSVRecord line : elements) {
                long index = line.getRecordNumber();
                try {
                    UserDTO userDTO = parseLine(line);
                    log.debug("userDTO:");
                    log.debug(userDTO.toString());
                    // Validate for errors
                    if (!validate(userDTO)) {
                        JSONObject error = new JSONObject();
                        error.put("index", index);
                        error.put("message", buildErrorString(userDTO));
                        errors.put(error);
                    } else {
                        log.debug("Looking for existing user: " + userDTO.getLogin());
                        ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(userDTO.getLogin());
                        if (response == null || !HttpStatus.CREATED.equals(response.getStatusCode())) {
                            
                        }
                        JSONObject existingUaaUser = new JSONObject(existingUserResponse.getBody());
                        String uaaUserLogin = existingUaaUser.getString("login");
                        // If user exists, update it
                        if (StringUtils.isNotBlank(uaaUserLogin)) {
                            // Update or create MemberSettings
                            if (memberSettingsExists(userDTO.getSalesforceId())) {
                                updateMemberSettings(userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead(), now);
                            } else {
                                createMemberSettings(userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead(), now);
                            }
                            // Update or create UserSettings
                            if (userSettingsExists(userDTO.getLogin())) {
                                updateUserSettings(userDTO, now);                                
                            } else {
                                createUserSettings(userDTO.getLogin(), userDTO.getSalesforceId(), false, now);
                            }
                        } else {
                            // Else create the user
                            createUserOnUAA(userDTO);
                            createUserSettings(userDTO.getLogin(), line.get("salesforceId"), false, now);
                            createMemberSettings(userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead(), now);
                        }
                    }
                } catch (Exception e) {
                    Throwable t = e.getCause();
                    JSONObject error = new JSONObject();
                    error.put("index", index);
                    if (t != null) {
                        log.error("Error on line " + index, t);
                        error.put("message", t.getMessage());
                    } else {
                        log.error("Error on line " + index, e);
                        error.put("message", e.getMessage());
                    }
                    errors.put(error);
                }
            }
        }
        return ResponseEntity.ok().body(errors.toString());
    }

    private UserDTO parseLine(CSVRecord record) {
        UserDTO u = new UserDTO();
        u.setLogin(record.get("email"));
        u.setFirstName(record.get("firstName"));
        u.setLastName(record.get("lastName"));
        u.setPassword(RandomStringUtils.randomAlphanumeric(10));
        String grants = record.get("grant");
        if (!StringUtils.isBlank(grants)) {
            if (!(grants.startsWith("[") && grants.endsWith("]"))) {
                throw new IllegalArgumentException("Grant list should start with '[' and ends with ']'");
            }
            List<String> authorities = Arrays.stream(grants.replace("[", "").replace("]", "").split(",")).collect(Collectors.toList());
            u.setAuthorities(authorities);
        }
        Boolean isConsortiumLead = StringUtils.isBlank(record.get("isConsortiumLead")) ? false : Boolean.parseBoolean(record.get("isConsortiumLead"));
        u.setIsConsortiumLead(isConsortiumLead);
        u.setSalesforceId(record.get("salesforceId"));
        if (!isConsortiumLead) {
            u.setParentSalesforceId(record.get("parentSalesforceId"));
        }
        return u;
    }

    private String buildErrorString(UserDTO userDTO) {
        StringBuilder error = new StringBuilder();
        if (userDTO.getLoginError() != null) {
            error.append(userDTO.getLoginError());
        }
        if (userDTO.getFirstNameError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getFirstNameError());
        }
        if (userDTO.getLastNameError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getLastNameError());
        }
        if (userDTO.getAuthoritiesError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getAuthoritiesError());
        }
        if (userDTO.getParentSalesforceIdError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getParentSalesforceIdError());
        }
        if (userDTO.getSalesforceIdError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getSalesforceIdError());
        }
        return error.toString();
    }

    /**
     * {@code POST  /user} : Create a new memberServicesUser.
     *
     * @param userDTO:
     *            the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with body the new user, or with status {@code 400 (Bad Request)}
     *         if the user has already an ID.
     * @throws Throwable
     */
    @PostMapping("/user")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) throws Throwable {
        log.debug("REST request to save UserDTO : {}", userDTO);
        if (!StringUtils.isBlank(userDTO.getId())) {
            throw new BadRequestAlertException("A new user cannot already have an ID", ENTITY_NAME, "idexists");
        }

        // Validate for errors
        if (!validate(userDTO)) {
            return ResponseEntity.badRequest().body(userDTO);
        }

        // Create the user on UAA
        JSONObject obj = createUserOnUAA(userDTO);
        String userLogin = obj.getString("login");
        String createdBy = SecurityUtils.getAuthenticatedUser();
        Instant createdDate = Instant.parse(obj.getString("createdDate"));
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        // Create the user settings
        UserSettings us = createUserSettings(userLogin, userDTO.getSalesforceId(), userDTO.getMainContact(), createdDate);

        // Create the member settings
        createMemberSettings(userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead(), createdDate);

        userDTO.setId(us.getId());
        userDTO.setLogin(userLogin);
        userDTO.setCreatedBy(createdBy);
        userDTO.setCreatedDate(createdDate);
        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);

        // Remove user password from response
        userDTO.setPassword(null);

        return ResponseEntity.created(new URI("/settings/api/user/" + us.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, us.getId().toString())).body(userDTO);
    }

    private boolean validate(UserDTO user) {
        boolean isOk = true;
        if (StringUtils.isBlank(user.getLogin())) {
            isOk = false;
            user.setLoginError("Login should not be empty");
        }
        if (StringUtils.isBlank(user.getSalesforceId())) {
            isOk = false;
            user.setSalesforceIdError("Salesforce Id should not be empty");
        }
        if (StringUtils.isBlank(user.getParentSalesforceId()) && BooleanUtils.isFalse(user.getIsConsortiumLead())) {
            isOk = false;
            user.setParentSalesforceIdError("Parent Salesforce Id should not be empty if it is not a consortium lead ");
        }
        return isOk;
    }

    private JSONObject createUserOnUAA(UserDTO userDTO) throws Throwable {
        String login = userDTO.getLogin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", login);
        map.put("password", userDTO.getPassword());
        map.put("email", login);
        if (userDTO.getAuthorities() == null) {
            userDTO.setAuthorities(new ArrayList<String>());
        }
        if (!userDTO.getAuthorities().contains("ROLE_USER")) {
            userDTO.getAuthorities().add("ROLE_USER");
        }
        map.put("authorities", userDTO.getAuthorities());
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());
        map.put("activated", false);

        try {
            ResponseEntity<Void> response = oauth2ServiceClient.registerUser(map);
            if (response == null || !HttpStatus.CREATED.equals(response.getStatusCode())) {
                throw new RuntimeException("User creation failed: " + response.getStatusCode().getReasonPhrase());
            }
        } catch (Exception rse) {
            if (rse.getCause() != null) {
                throw rse.getCause();
            } else {
                throw rse;
            }
        }

        // Now fetch the user to get the user id and populate the member
        // services user information
        ResponseEntity<String> userInfo = oauth2ServiceClient.getUser(login);
        return new JSONObject(userInfo.getBody());
    }

    private Boolean memberSettingsExists(String salesforceId) {
        Optional<MemberSettings> existingMemberSettings = memberSettingsRepository.findBySalesforceId(salesforceId);
        return existingMemberSettings.isPresent();
    }

    private MemberSettings createMemberSettings(String salesforceId, String parentSalesforceId, Boolean isConsortiumLead, Instant now) throws JSONException {
        log.info("Creating MemberSettings with Salesforce Id {}", salesforceId);
        MemberSettings ms = new MemberSettings();
        String createdBy = SecurityUtils.getAuthenticatedUser();
        ms.setSalesforceId(salesforceId);
        ms.setParentSalesforceId(parentSalesforceId);
        ms.setIsConsortiumLead((isConsortiumLead == null) ? false : isConsortiumLead);
        ms.setCreatedBy(createdBy);
        ms.setCreatedDate(now);
        ms.setLastModifiedBy(createdBy);
        ms.setLastModifiedDate(now);
        return memberSettingsRepository.save(ms);
    }

    private MemberSettings updateMemberSettings(String salesforceId, String parentSalesforceId, Boolean isConsortiumLead, Instant lastModifiedDate) {
        log.info("Updating MemberSettings with Salesforce Id {}", salesforceId);
        Optional<MemberSettings> existingMemberSettings = memberSettingsRepository.findBySalesforceId(salesforceId);
        MemberSettings ms = existingMemberSettings.get();
        ms.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
        ms.setLastModifiedDate(lastModifiedDate);
        ms.setParentSalesforceId(parentSalesforceId);
        ms.setIsConsortiumLead((isConsortiumLead == null) ? false : isConsortiumLead);
        return memberSettingsRepository.save(ms);
    }

    /**
     * {@code PUT  /user} : Updates an existing memberServicesUser.
     *
     * @param userDTO
     *            the User to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the updated User, or with status {@code 400 (Bad Request)}
     *         if the user is not valid, or with status
     *         {@code 500 (Internal Server Error)} if the user couldn't be
     *         updated.
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect.
     * @throws JSONException
     */
    @PutMapping("/user")
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException, JSONException {
        // TODO: Secure this endpoint, only admins or the user himself should be
        // able to update a user
        log.debug("REST request to update UserDTO : {}", userDTO);
        if (StringUtils.isBlank(userDTO.getId()) || StringUtils.isBlank(userDTO.getLogin())) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        // Verify the user exists on the UAA table
        ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(userDTO.getLogin());
        JSONObject existingUaaUser = new JSONObject(existingUserResponse.getBody());

        String uaaUserLogin = existingUaaUser.getString("login");
        // userLogin must match
        if (!userDTO.getLogin().equals(uaaUserLogin)) {
            throw new RuntimeException("User login doesn't match: " + userDTO.getLogin() + " - " + uaaUserLogin);
        }

        // Update jhi_user entry
        JSONObject obj = updateUserOnUAA(existingUaaUser.getString("id"), userDTO);
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        // Update UserSettings
        updateUserSettings(userDTO, lastModifiedDate);

        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userDTO.getId().toString())).body(userDTO);
    }

    private JSONObject updateUserOnUAA(String id, UserDTO userDTO) throws JSONException {
        // Update jhi_user entry
        Map<String, Object> map = new HashMap<String, Object>();

        // Remember to use the UAA user id, since we are updating the user info
        map.put("id", id);
        map.put("login", userDTO.getLogin());
        map.put("password", "requires_not_empty_but_doesnt_get_updated");
        map.put("email", userDTO.getLogin());
        map.put("authorities", userDTO.getAuthorities());
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Update user failed: " + response.getStatusCode().getReasonPhrase());
        }

        return new JSONObject(response.getBody());
    }

    private Boolean userSettingsExists(String login) {
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findByLogin(login);
        return existingUserSettingsOptional.isPresent();
    }

    private UserSettings createUserSettings(String userLogin, String salesforceId, Boolean mainContact, Instant now) throws JSONException {
        log.info("Creating userSettings for: " + userLogin);
        String createdBy = SecurityUtils.getAuthenticatedUser();

        UserSettings us = new UserSettings();
        us.setLogin(userLogin);
        us.setMainContact(mainContact);
        us.setSalesforceId(salesforceId);
        us.setCreatedBy(createdBy);
        us.setCreatedDate(now);
        us.setLastModifiedBy(createdBy);
        us.setLastModifiedDate(now);

        // Persist it
        return userSettingsRepository.save(us);
    }

    private void updateUserSettings(UserDTO userDTO, Instant lastModifiedDate) {
        log.info("Updating userSettings for: " + userDTO.toString());
        // Verify the user exists on the UserSettings table
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findByLogin(userDTO.getLogin());
        if (!existingUserSettingsOptional.isPresent()) {
            throw new BadRequestAlertException("Invalid login, unable to find UserSettings for " + userDTO.getLogin(), ENTITY_NAME, "id null");
        }
        Boolean userSettingsModified = false;
        UserSettings existingUserSettings = existingUserSettingsOptional.get();
        Boolean existingMainContactFlag = existingUserSettings.getMainContact();
        if (existingMainContactFlag != userDTO.getMainContact()) {
            existingUserSettings.setMainContact(userDTO.getMainContact());
            userSettingsModified = true;
        }

        // Check if salesforceId changed
        String existingSalesforceId = existingUserSettings.getSalesforceId();
        if (!existingSalesforceId.equals(userDTO.getSalesforceId())) {
            // Check if new salesforceId exists in MemberSettings
            if (!memberSettingsRepository.existsBySalesforceId(userDTO.getSalesforceId())) {
                throw new BadRequestAlertException("Invalid salesforceId, there is not MemberSettings entry for " + userDTO.getSalesforceId(), ENTITY_NAME,
                        "salesforceIdInvalid");
            }
            existingUserSettings.setSalesforceId(userDTO.getSalesforceId());
            userSettingsModified = true;
        }

        // Check if main contact changed
        if (Boolean.compare(existingUserSettings.getMainContact(), userDTO.getMainContact()) != 0) {
            existingUserSettings.setMainContact(userDTO.getMainContact());
            userSettingsModified = true;
        }

        // Update UserSettings
        if (userSettingsModified) {
            existingUserSettings.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
            existingUserSettings.setLastModifiedDate(lastModifiedDate);
            userSettingsRepository.save(existingUserSettings);
        }
    }

    /**
     * {@code GET  /users} : get all the memberServicesUsers.
     *
     * @param pageable
     *            the pagination information.
     * @param queryParams
     *            a {@link MultiValueMap} query parameters.
     * @param uriBuilder
     *            a {@link UriComponentsBuilder} URI builder.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the
     *         list of Users in body.
     * @throws JSONException
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws JSONException {
        log.debug("REST request to get a page of users");
        Page<UserSettings> page = userSettingsRepository.findAll(pageable);
        List<UserDTO> dtoList = new ArrayList<UserDTO>();

        for (UserSettings us : page) {
            dtoList.add(populateDTO(us));
        }

        Page<UserDTO> dtoPage = new PageImpl<UserDTO>(dtoList, page.getPageable(), page.getTotalElements());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), dtoPage);
        return ResponseEntity.ok().headers(headers).body(dtoPage.getContent());
    }

    /**
     * {@code GET  /user/:login} : get the "login" User.
     *
     * @param id
     *            the id of the User to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the User, or with status {@code 404 (Not Found)}.
     * @throws JSONException
     */
    @GetMapping("/user/{login}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String login) throws JSONException {
        // TODO: Secure this endpoint, we should make it available for the admin
        // or the logged in user IF it is the same user as the one specified in
        // the id param
        log.debug("REST request to get UserDTO : {}", login);
        Optional<UserSettings> msu = userSettingsRepository.findByLogin(login);
        if (!msu.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // UserSettings data
        UserDTO dto = populateDTO(msu.get());

        return ResponseEntity.ok().body(dto);
    }

    private UserDTO populateDTO(UserSettings us) throws JSONException {
        UserDTO u = UserDTO.valueOf(us);
        // UAA data
        ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(us.getLogin());
        JSONObject existingUser = new JSONObject(existingUserResponse.getBody());
        u.setFirstName(existingUser.getString("firstName"));
        u.setLastName(existingUser.getString("lastName"));
        u.setLogin(existingUser.getString("login"));
        List<String> authorities = new ArrayList<String>();
        JSONArray array = existingUser.getJSONArray("authorities");
        for (int i = 0; i < array.length(); i++) {
            authorities.add(array.getString(i));
        }
        u.setAuthorities(authorities);

        // MemberSettings data
        Optional<MemberSettings> oms = memberSettingsRepository.findBySalesforceId(us.getSalesforceId());
        if (oms.isPresent()) {
            MemberSettings ms = oms.get();
            u.setSalesforceId(ms.getSalesforceId());
            u.setParentSalesforceId(ms.getParentSalesforceId());
            u.setIsConsortiumLead(ms.getIsConsortiumLead());
        }
        return u;
    }

    /**
     * {@code DELETE  /users/:id} : disable the "id" User.
     *
     * @param id
     *            the id of the User to disable.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     * @throws JSONException
     */
    @DeleteMapping("/user/{id}/{authority}")
    public ResponseEntity<Void> removeAuthority(@PathVariable String id, @PathVariable String authority) throws JSONException {
        log.debug("REST request to remove authority {} from user {}", authority, id);

        Optional<UserSettings> ous = userSettingsRepository.findById(id);
        if (!ous.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        UserSettings us = ous.get();

        // Now fetch the user to get the user id and populate the member
        // services user information
        ResponseEntity<String> userInfo = oauth2ServiceClient.getUser(us.getLogin());

        if (HttpStatus.NOT_FOUND.equals(userInfo.getStatusCode())) {
            throw new RuntimeException("User not found: " + us.getLogin());
        }
        String user = userInfo.getBody();

        JSONObject obj = new JSONObject(user);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", obj.getString("id"));
        map.put("login", obj.getString("login"));
        map.put("firstName", obj.getString("firstName"));
        map.put("lastName", obj.getString("lastName"));
        map.put("email", obj.getString("email"));
        map.put("password", "requires_not_empty_but_doesnt_get_updated");
        map.put("imageUrl", obj.getString("imageUrl"));
        map.put("activated", obj.getBoolean("activated"));
        map.put("langKey", obj.getString("langKey"));

        List<String> authorities = new ArrayList<String>();
        JSONArray array = obj.getJSONArray("authorities");
        for (int i = 0; i < array.length(); i++) {
            String authName = array.getString(i);
            if (!authority.equalsIgnoreCase(authName)) {
                authorities.add(authName);
            }
        }
        map.put("authorities", authorities);

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Unable to remove authority " + authority + " from user " + us.getLogin() + ": " + response.getStatusCode().getReasonPhrase());
        }

        return ResponseEntity.accepted().build();
    }
}
