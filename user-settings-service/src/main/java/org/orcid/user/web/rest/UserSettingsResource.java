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
     * {@code POST  /users} : Create a list of users.
     *
     * @param usersFile:
     *            file containing the users to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with a map indicating if each user was created or not, or with
     *         status {@code 400 (Bad Request)} if the file cannot be parsed.
     * @throws Throwable
     */
    @PostMapping("/user/import")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<Map<Long, String>> createUsers(@RequestParam("file") MultipartFile file) throws Throwable {
        Map<Long, String> userIds = new HashMap<Long, String>();
        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            for (CSVRecord line : elements) {
                long index = line.getRecordNumber();
                try {
                    UserDTO userDTO = parseLine(line);
                    // Validate for errors
                    if (!validate(userDTO)) {
                        userIds.put(index, buildErrorString(userDTO));
                    }
                    JSONObject obj = createUserOnUAA(userDTO);
                    createUserSettings(obj, line.get("salesforceId"), false);
                    createMemberSettings(obj, userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead());
                    userIds.put(index, obj.getString("id"));
                } catch (Exception e) {
                    Throwable t = e.getCause();
                    if (t != null) {
                        log.error("Error on line " + index, t.getMessage());
                        userIds.put(index, t.getMessage());
                    } else {
                        log.error("Error on line " + index, e.getMessage());
                        userIds.put(index, e.getMessage());
                    }
                }
            }
        }
        return ResponseEntity.ok().body(userIds);
    }

    private UserDTO parseLine(CSVRecord record) {
        UserDTO u = new UserDTO();
        u.setLogin(record.get("email"));
        u.setEmail(record.get("email"));
        u.setFirstName(record.get("firstName"));
        u.setLastName(record.get("lastName"));
        u.setPassword(RandomStringUtils.random(10));
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
        if (userDTO.getEmailError() != null) {
            if (error.length() > 0)
                error.append(", ");
            error.append(userDTO.getEmailError());
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
        String createdBy = obj.getString("createdBy");
        Instant createdDate = Instant.parse(obj.getString("createdDate"));
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        // Create the user settings
        UserSettings us = createUserSettings(obj, userDTO.getSalesforceId(), userDTO.getMainContact());

        // Create the member settings
        createMemberSettings(obj, userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead());

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
        if (StringUtils.isBlank(user.getEmail())) {
            isOk = false;
            user.setEmailError("Email should not be empty");
        }
        if (user.getAuthorities() == null || user.getAuthorities().isEmpty()) {
            isOk = false;
            user.setAuthoritiesError("You should specify at least one authority");
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
        map.put("email", userDTO.getEmail());
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
        String user = userInfo.getBody();
        System.out.println(user);
        return new JSONObject(user);
    }

    private UserSettings createUserSettings(JSONObject obj, String salesforceId, Boolean mainContact) throws JSONException {
        String userLogin = obj.getString("login");
        String createdBy = obj.getString("createdBy");
        Instant createdDate = Instant.parse(obj.getString("createdDate"));
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        UserSettings us = new UserSettings();
        us.setLogin(userLogin);
        us.setMainContact(mainContact);
        us.setSalesforceId(salesforceId);
        us.setCreatedBy(createdBy);
        us.setCreatedDate(createdDate);
        us.setLastModifiedBy(lastModifiedBy);
        us.setLastModifiedDate(lastModifiedDate);

        // Persist it
        return userSettingsRepository.save(us);
    }

    private MemberSettings createMemberSettings(JSONObject obj, String salesforceId, String parentSalesforceId, Boolean isConsortiumLead) throws JSONException {
        Optional<MemberSettings> existingMemberSettings = memberSettingsRepository.findBySalesforceId(salesforceId);
        MemberSettings ms;
        // Check if member settings already exists
        if (!existingMemberSettings.isPresent()) {
            log.info("Creating MemberSettings with Salesforce Id {}", salesforceId);
            String createdBy = obj.getString("createdBy");
            Instant createdDate = Instant.parse(obj.getString("createdDate"));
            String lastModifiedBy = obj.getString("lastModifiedBy");
            Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

            ms = new MemberSettings();
            ms.setSalesforceId(salesforceId);
            ms.setParentSalesforceId(parentSalesforceId);
            ms.setIsConsortiumLead((isConsortiumLead == null) ? false : isConsortiumLead);
            ms.setCreatedBy(createdBy);
            ms.setCreatedDate(createdDate);
            ms.setLastModifiedBy(lastModifiedBy);
            ms.setLastModifiedDate(lastModifiedDate);
            return memberSettingsRepository.save(ms);
        } else {
            return existingMemberSettings.get();
        }
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

        // Verify the user exists on the UserSettings table
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findByLogin(userDTO.getLogin());
        if (!existingUserSettingsOptional.isPresent()) {
            throw new BadRequestAlertException("Invalid login, unable to find UserSettings for " + userDTO.getId(), ENTITY_NAME, "idnull");
        }

        // Update jhi_user entry
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("id", userDTO.getId());
        map.put("login", userDTO.getLogin());
        map.put("password", userDTO.getPassword());
        map.put("email", userDTO.getEmail());
        map.put("authorities", userDTO.getAuthorities());
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Update user failed: " + response.getStatusCode().getReasonPhrase());
        }

        JSONObject obj = new JSONObject(response.getBody());
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

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

        // Update UserSettings
        if (userSettingsModified) {
            existingUserSettings.setLastModifiedBy(lastModifiedBy);
            existingUserSettings.setLastModifiedDate(lastModifiedDate);
            userSettingsRepository.save(existingUserSettings);
        }
        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userDTO.getId().toString())).body(userDTO);
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
     * {@code GET  /users/:id} : get the "id" User.
     *
     * @param id
     *            the id of the User to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the User, or with status {@code 404 (Not Found)}.
     * @throws JSONException 
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) throws JSONException {
        log.debug("REST request to get UserDTO : {}", id);
        Optional<UserSettings> msu = userSettingsRepository.findById(id);
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
        u.setEmail(existingUser.getString("email"));
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
    @DeleteMapping("/user/{login}/{authority}")
    public ResponseEntity<Void> disableUser(@PathVariable String login, @PathVariable String authority) throws JSONException {
        log.debug("REST request to remove authority {} from user {}", authority, login);

        // Now fetch the user to get the user id and populate the member
        // services user information
        ResponseEntity<String> userInfo = oauth2ServiceClient.getUser(login);

        if (HttpStatus.NOT_FOUND.equals(userInfo.getStatusCode())) {
            throw new RuntimeException("User not found: " + login);
        }
        String user = userInfo.getBody();

        JSONObject obj = new JSONObject(user);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", login);
        map.put("password", obj.getString(""));
        map.put("email", obj.getString(""));

        map.put("firstName", obj.getString(""));
        map.put("lastName", obj.getString(""));
        map.put("imageUrl", obj.getString(""));
        map.put("activated", obj.getString(""));
        map.put("langKey", obj.getString(""));

        // TODO: process authorities, remove the one indicated
        map.put("authorities", obj.getString(""));

        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, login)).build();
    }
}
