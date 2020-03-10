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
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.AuthoritiesConstants;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
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

    public void setOauth2ServiceClient(Oauth2ServiceClient oauth2ServiceClient) {
        this.oauth2ServiceClient = oauth2ServiceClient;
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
                processElement(line, now, errors);
            }
        }
        return ResponseEntity.ok().body(errors.toString());
    }

    private void processElement(CSVRecord element, Instant now, JSONArray errors) throws Throwable {
        long index = element.getRecordNumber();
        String errorString = new String();
        try {
            // Validate for errors
            if (!validate(element, errorString)) {
                JSONObject error = new JSONObject();
                error.put("index", index);
                error.put("message", errorString);
                errors.put(error);
            } else {
                String login = element.get("email");
                log.debug("Looking for existing user: " + login);
                JSONObject existingUaaUser = getUAAUserByLogin(login);
                String salesforceId = element.get("salesforceId");
                String parentSalesforceId = element.get("parentSalesforceId");
                Boolean isConsortiumLead = StringUtils.isBlank(element.get("isConsortiumLead")) ? false : Boolean.parseBoolean(element.get("isConsortiumLead"));
                UserDTO userDTO = getUserDTO(element);
                // If user exists, update it
                if (existingUaaUser != null) {
                    // Updates the UAA user
                    JSONObject uaaUser = updateUserOnUAA(userDTO, existingUaaUser);
                    String jhiUserId = uaaUser.getString("id");
                    // Update or create MemberSettings
                    if (memberSettingsExists(salesforceId)) {
                        updateMemberSettings(salesforceId, parentSalesforceId, isConsortiumLead, now);
                    } else {
                        createMemberSettings(salesforceId, parentSalesforceId, isConsortiumLead, now);
                    }
                    // Update or create UserSettings
                    if (userSettingsExists(uaaUser.getString("id"))) {
                        updateUserSettings(userDTO, now);
                    } else {
                        createUserSettings(jhiUserId, salesforceId, false, now);
                    }
                } else {
                    // Else create the user
                    JSONObject uaaUser = createUserOnUAA(userDTO);
                    createUserSettings(uaaUser.getString("id"), element.get("salesforceId"), false, now);
                    createMemberSettings(salesforceId, parentSalesforceId, isConsortiumLead, now);
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

    private UserDTO getUserDTO(CSVRecord record) {
        UserDTO u = new UserDTO();
        u.setLogin(record.get("email"));
        u.setFirstName(record.get("firstName"));
        u.setLastName(record.get("lastName"));
        u.setSalesforceId(record.get("salesforceId"));
        u.setPassword(RandomStringUtils.randomAlphanumeric(10));
        List<String> authorities = new ArrayList<String>();
        String grants = record.get("grant");
        if (!StringUtils.isBlank(grants)) {
            if (!(grants.startsWith("[") && grants.endsWith("]"))) {
                throw new IllegalArgumentException("Grant list should start with '[' and ends with ']'");
            }
            authorities = Arrays.stream(grants.replace("[", "").replace("]", "").split(",")).collect(Collectors.toList());
        }
        if (authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
            u.setAssertionServicesEnabled(true);
        }
        return u;
    }

    private JSONObject getUAAUserByLogin(String login) throws JSONException {
        return getUAAUser(login, false);
    }

    private JSONObject getUAAUserById(String id) throws JSONException {
        return getUAAUser(id, true);
    }

    private JSONObject getUAAUser(String loginOrId, boolean isId) throws JSONException {
        JSONObject existingUaaUser = null;
        try {
            ResponseEntity<String> existingUserResponse = null;
            if (isId) {
                System.out.println("Getting UAA user by id");
                existingUserResponse = oauth2ServiceClient.getUserById(loginOrId);
            } else {
                existingUserResponse = oauth2ServiceClient.getUser(loginOrId);
            }
            log.debug("Status code: " + existingUserResponse.getStatusCodeValue());
            if (existingUserResponse != null) {
                existingUaaUser = new JSONObject(existingUserResponse.getBody());
            }
        } catch (HystrixRuntimeException hre) {
            if (hre.getCause() != null && ResponseStatusException.class.isAssignableFrom(hre.getCause().getClass())) {
                ResponseStatusException rse = (ResponseStatusException) hre.getCause();
                if (HttpStatus.NOT_FOUND.equals(rse.getStatus())) {
                    log.debug("User not found: " + loginOrId);
                } else {
                    throw hre;
                }
            }
        }
        return existingUaaUser;
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

        // Validate user does not exists
        ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(userDTO.getLogin());
        if (!existingUserResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.debug("User '{}' couldn't be created because it already exists, status code {}", userDTO.getLogin(), existingUserResponse.getStatusCode());
            return ResponseEntity.badRequest().body(userDTO);
        }

        // Create the user on UAA
        JSONObject obj = createUserOnUAA(userDTO);
        String userIdOnUAA = obj.getString("id");
        String userLogin = obj.getString("login");
        String createdBy = SecurityUtils.getAuthenticatedUser();
        Instant createdDate = Instant.parse(obj.getString("createdDate"));
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        // Create the user settings
        UserSettings us = createUserSettings(userIdOnUAA, userDTO.getSalesforceId(), userDTO.getMainContact(), createdDate);

        // Create the member settings
        createMemberSettings(userDTO.getSalesforceId(), StringUtils.EMPTY, false, createdDate);

        userDTO.setId(us.getId());
        userDTO.setLogin(userLogin);
        userDTO.setJhiUserId(userIdOnUAA);
        userDTO.setCreatedBy(createdBy);
        userDTO.setCreatedDate(createdDate);
        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);

        // Remove user password from response
        userDTO.setPassword(null);

        return ResponseEntity.created(new URI("/settings/api/user/" + us.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, us.getId().toString())).body(userDTO);
    }

    private boolean validate(CSVRecord record, String error) {
        boolean isOk = true;
        if (StringUtils.isBlank(record.get("email"))) {
            isOk = false;
            error = "Login should not be empty";
        }
        if (StringUtils.isBlank(record.get("salesforceId"))) {
            if (!isOk) {
                error += ", ";
            }
            isOk = false;
            error += "Salesforce Id should not be empty";
        }
        Boolean isConsortiumLead = StringUtils.isBlank(record.get("isConsortiumLead")) ? false : Boolean.parseBoolean(record.get("isConsortiumLead"));

        if (StringUtils.isBlank(record.get("parentSalesforceId")) && BooleanUtils.isFalse(isConsortiumLead)) {
            if (!isOk) {
                error += ", ";
            }
            isOk = false;
            error += "Parent Salesforce Id should not be empty if it is not a consortium lead";
        }
        return isOk;
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
        return isOk;
    }

    private JSONObject createUserOnUAA(UserDTO userDTO) throws Throwable {
        String login = userDTO.getLogin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", login);
        map.put("password", userDTO.getPassword());
        map.put("email", login);
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());
        map.put("activated", false);

        List<String> authorities = new ArrayList<String>();
        authorities.add(AuthoritiesConstants.USER);
        if (userDTO.getAssertionServicesEnabled() != null && userDTO.getAssertionServicesEnabled()) {
            authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
        }
        map.put("authorities", authorities);

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
        String createdBy = getJhiUserId(SecurityUtils.getAuthenticatedUser());
        ms.setSalesforceId(salesforceId);
        ms.setParentSalesforceId(parentSalesforceId);
        ms.setIsConsortiumLead((isConsortiumLead == null) ? false : isConsortiumLead);
        ms.setCreatedBy(createdBy);
        ms.setCreatedDate(now);
        ms.setLastModifiedBy(createdBy);
        ms.setLastModifiedDate(now);
        return memberSettingsRepository.save(ms);
    }

    private MemberSettings updateMemberSettings(String salesforceId, String parentSalesforceId, Boolean isConsortiumLead, Instant lastModifiedDate) throws JSONException {
        log.info("Updating MemberSettings with Salesforce Id {}", salesforceId);
        Optional<MemberSettings> existingMemberSettings = memberSettingsRepository.findBySalesforceId(salesforceId);
        MemberSettings ms = existingMemberSettings.get();
        String lastModifiedBy = getJhiUserId(SecurityUtils.getAuthenticatedUser());
        ms.setParentSalesforceId(parentSalesforceId);
        ms.setIsConsortiumLead((isConsortiumLead == null) ? false : isConsortiumLead);
        ms.setLastModifiedBy(lastModifiedBy);
        ms.setLastModifiedDate(lastModifiedDate);
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
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException, JSONException {
        log.debug("REST request to update UserDTO : {}", userDTO);
        if (StringUtils.isBlank(userDTO.getId()) || StringUtils.isBlank(userDTO.getLogin())) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        // Verify the user exists on the UAA table
        JSONObject existingUaaUser = getUAAUserByLogin(userDTO.getLogin());

        String uaaUserLogin = existingUaaUser.getString("login");
        // userLogin must match
        if (!userDTO.getLogin().equals(uaaUserLogin)) {
            throw new RuntimeException("User login doesn't match: " + userDTO.getLogin() + " - " + uaaUserLogin);
        }

        // Update jhi_user entry
        JSONObject obj = updateUserOnUAA(userDTO, existingUaaUser);
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        // Update UserSettings
        updateUserSettings(userDTO, lastModifiedDate);

        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userDTO.getId().toString())).body(userDTO);
    }

    private JSONObject updateUserOnUAA(UserDTO userDTO, JSONObject existingUaaUser) throws JSONException {
        // Update jhi_user entry
        Map<String, Object> map = new HashMap<String, Object>();

        // Remember to use the UAA user id, since we are updating the user info
        map.put("id", existingUaaUser.getString("id"));
        map.put("login", existingUaaUser.getString("login"));
        map.put("email", existingUaaUser.getString("login"));
        map.put("password", "requires_not_empty_but_doesnt_get_updated");
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());

        List<String> authList = new ArrayList<String>();
        JSONArray existingAuth = existingUaaUser.getJSONArray("authorities");
        for (int i = 0; i < existingAuth.length(); i++) {
            authList.add(existingAuth.getString(i));
        }
        if (userDTO.getAssertionServicesEnabled() != null && userDTO.getAssertionServicesEnabled()
                && !authList.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
            authList.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
        }

        map.put("authorities", authList);
        // TODO: We should be able to set the activated flag from here
        map.put("activated", existingUaaUser.getString("activated"));
        // TODO: We should be able to set the lang key from here
        map.put("langKey", existingUaaUser.getString("langKey"));

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Update user failed: " + response.getStatusCode().getReasonPhrase());
        }

        return new JSONObject(response.getBody());
    }

    private Boolean userSettingsExists(String jhiUserId) {
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findByJhiUserId(jhiUserId);
        return existingUserSettingsOptional.isPresent();
    }

    private UserSettings createUserSettings(String jhiUserId, String salesforceId, Boolean mainContact, Instant now) throws JSONException {
        log.info("Creating userSettings for: " + jhiUserId);
        String createdBy = getJhiUserId(SecurityUtils.getAuthenticatedUser());

        UserSettings us = new UserSettings();
        us.setJhiUserId(jhiUserId);
        us.setMainContact(mainContact);
        us.setSalesforceId(salesforceId);
        us.setCreatedBy(createdBy);
        us.setCreatedDate(now);
        us.setLastModifiedBy(createdBy);
        us.setLastModifiedDate(now);

        // Persist it
        return userSettingsRepository.save(us);
    }

    private void updateUserSettings(UserDTO userDTO, Instant lastModifiedDate) throws JSONException {
        log.info("Updating userSettings for: " + userDTO.toString());
        // Verify the user exists on the UserSettings table
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findByJhiUserId(userDTO.getJhiUserId());
        if (!existingUserSettingsOptional.isPresent()) {
            throw new BadRequestAlertException("Invalid login, unable to find UserSettings for " + userDTO.getLogin(), ENTITY_NAME, "id null");
        }
        Boolean userSettingsModified = false;
        UserSettings existingUserSettings = existingUserSettingsOptional.get();
        Boolean existingMainContactFlag = existingUserSettings.getMainContact();
        if (userDTO.getMainContact() != null && existingMainContactFlag != userDTO.getMainContact()) {
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
        if ((existingUserSettings.getMainContact() == null) || !(existingUserSettings.getMainContact().equals(userDTO.getMainContact()))) {
            existingUserSettings.setMainContact(userDTO.getMainContact());
            userSettingsModified = true;
        }

        // Update UserSettings
        if (userSettingsModified) {
            existingUserSettings.setLastModifiedBy(getJhiUserId(SecurityUtils.getAuthenticatedUser()));
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
    @GetMapping("/user/{jhiUserId}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String jhiUserId) throws JSONException {
        // TODO: Secure this endpoint, we should make it available for the admin
        // or the logged in user IF it is the same user as the one specified in
        // the id param
        log.debug("REST request to get UserDTO : {}", jhiUserId);
        Optional<UserSettings> ous = userSettingsRepository.findByJhiUserId(jhiUserId);
        if (!ous.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // UserSettings data
        UserDTO dto = populateDTO(ous.get());

        return ResponseEntity.ok().body(dto);
    }

    private UserDTO populateDTO(UserSettings us) throws JSONException {
        UserDTO u = UserDTO.valueOf(us);
        // UAA data
        System.out.println("jhiUserId? " + us.getJhiUserId());
        JSONObject existingUaaUser = getUAAUserById(us.getJhiUserId());
        
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println("The user is null? " + (existingUaaUser == null));
        System.out.println(existingUaaUser.toString());
        System.out.println("-----------------------------------------------------------------------------------------");
        
        u.setFirstName(existingUaaUser.getString("firstName"));
        u.setLastName(existingUaaUser.getString("lastName"));
        u.setLogin(existingUaaUser.getString("login"));
        JSONArray array = existingUaaUser.getJSONArray("authorities");
        for (int i = 0; i < array.length(); i++) {
            String authority = array.getString(i);
            if (AuthoritiesConstants.ASSERTION_SERVICE_ENABLED.equals(authority)) {
                u.setAssertionServicesEnabled(true);
            }
        }
        return u;
    }

    /**
     * {@code DELETE  /users/:login} : delete the 'login' user.
     *
     * @param login
     *            the id of the User to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     * @throws JSONException
     */
    @DeleteMapping("/user/{login}")
    public ResponseEntity<Void> deleteUser(@PathVariable String login) throws JSONException {
        log.debug("REST request to delete user {}", login);

        JSONObject uaaUser = getUAAUserByLogin(login);
        // Empty user on UAA
        Map<String, Object> map = new HashMap<String, Object>();

        // Remember to use the UAA user id, since we are updating the user info
        map.put("id", uaaUser.getString("id"));
        map.put("login", StringUtils.EMPTY);
        map.put("email", StringUtils.EMPTY);
        map.put("password", "requires_not_empty_but_doesnt_get_updated");
        map.put("firstName", StringUtils.EMPTY);
        map.put("lastName", StringUtils.EMPTY);

        map.put("authorities", new ArrayList<String>());
        map.put("activated", false);

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Delete user failed: " + response.getStatusCode().getReasonPhrase());
        }

        Optional<UserSettings> ous = userSettingsRepository.findById(login);
        if (!ous.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        UserSettings us = ous.get();
        us.setSalesforceId(null);
        us.setMainContact(null);

        userSettingsRepository.save(us);

        return ResponseEntity.accepted().build();
    }

    /**
     * {@code DELETE  /users/:id/:authority} : remove the authority from the
     * given user.
     *
     * @param id
     *            the id of the User.
     * @param authority
     *            the authority to be removed
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

        JSONObject obj = getUAAUserById(us.getJhiUserId());

        if (obj == null || obj.isNull("login")) {
            // TODO: handle exception properly
            throw new RuntimeException("Unable to find user with ID: " + id);
        }

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
            throw new RuntimeException("Unable to remove authority " + authority + " from user " + us.getId() + ": " + response.getStatusCode().getReasonPhrase());
        }

        return ResponseEntity.accepted().build();
    }

    private String getJhiUserId(String userLogin) throws JSONException {
        JSONObject uaaUser = getUAAUserByLogin(userLogin);
        log.debug(uaaUser.toString());
        return uaaUser.getString("id");
    }
}
