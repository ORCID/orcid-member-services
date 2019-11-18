package org.orcid.user.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
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
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect.
     * @throws JSONException
     * @throws ParseException
     * @throws IOException
     */
    @PostMapping("/user/import")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<Map<Integer, Boolean>> createUsers(@RequestParam("file") MultipartFile file)
            throws URISyntaxException, JSONException, ParseException, IOException {
        // TODO: see
        // https://github.com/ORCID/orcid-assertion-service/blob/master/src/main/java/org/orcid/assertionService/web/rest/AffiliationResource.java#L222

        try (InputStream is = file.getInputStream();) {
            InputStreamReader isr = new InputStreamReader(is);
            Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);
            // Validate affiliations
            for (CSVRecord line : elements) {
                try {
                    JSONObject obj = createUserOnUAA(parseLine(line));
                    createUserSettings(obj, line.get("salesforceId"), Boolean.valueOf(line.get("mainContact")));
                    createMemberSettings(salesforceId, parentSalesforceId, isConsortiumLead, obj)
                } catch (Exception e) {

                }
            }
        }
        return null;
    }

    private UserDTO parseLine(CSVRecord record) {
        UserDTO u = new UserDTO();
        u.setLogin(record.get("email"));
        u.setEmail(record.get("email"));
        u.setFirstName(record.get("firstName"));
        u.setLastName(record.get("lastName"));
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

    /**
     * {@code POST  /user} : Create a new memberServicesUser.
     *
     * @param userDTO:
     *            the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with body the new user, or with status {@code 400 (Bad Request)}
     *         if the user has already an ID.
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect.
     * @throws JSONException
     * @throws ParseException
     */
    @PostMapping("/user")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException, JSONException, ParseException {
        log.debug("REST request to save UserDTO : {}", userDTO);
        if (!StringUtils.isBlank(userDTO.getId())) {
            throw new BadRequestAlertException("A new user cannot already have an ID", ENTITY_NAME, "idexists");
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
        MemberSettings m = createMemberSettings(userDTO.getSalesforceId(), userDTO.getParentSalesforceId(), userDTO.getIsConsortiumLead(), obj);
        
        userDTO.setId(us.getId());
        userDTO.setLogin(userLogin);
        userDTO.setCreatedBy(createdBy);
        userDTO.setCreatedDate(createdDate);
        userDTO.setLastModifiedBy(lastModifiedBy);
        userDTO.setLastModifiedDate(lastModifiedDate);

        return ResponseEntity.created(new URI("/settings/api/users/" + us.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, us.getId().toString())).body(userDTO);
    }
    
    private JSONObject createUserOnUAA(UserDTO userDTO) throws JSONException {
        String login = userDTO.getLogin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("login", login);
        map.put("password", userDTO.getPassword());
        map.put("email", userDTO.getEmail());
        map.put("authorities", userDTO.getAuthorities());
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());
        map.put("activated", false);

        ResponseEntity<Void> response = oauth2ServiceClient.registerUser(map);
        if (response == null || !HttpStatus.CREATED.equals(response.getStatusCode())) {
            throw new RuntimeException("User creation failed: " + response.getStatusCode().getReasonPhrase());
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
        us.setMainContact(false);
        us.setSalesforceId(salesforceId);
        us.setCreatedBy(createdBy);
        us.setCreatedDate(createdDate);
        us.setLastModifiedBy(lastModifiedBy);
        us.setLastModifiedDate(lastModifiedDate);

        // Persist it
        return userSettingsRepository.save(us);
    }

    private MemberSettings createMemberSettings(String salesforceId, String parentSalesforceId, Boolean isConsortiumLead, JSONObject obj) throws JSONException {
        String createdBy = obj.getString("createdBy");
        Instant createdDate = Instant.parse(obj.getString("createdDate"));
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));
        
        MemberSettings m = new MemberSettings();
        m.setSalesforceId(salesforceId);
        m.setParentSalesforceId(parentSalesforceId);
        m.setIsConsortiumLead(isConsortiumLead);
        m.setCreatedBy(createdBy);
        m.setCreatedDate(createdDate);
        m.setLastModifiedBy(lastModifiedBy);
        m.setLastModifiedDate(lastModifiedDate);
        
        return memberSettingsRepository.save(m);
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
        Optional<UserSettings> existingUserSettingsOptional = userSettingsRepository.findById(userDTO.getId());
        if(!existingUserSettingsOptional.isPresent()) {
            throw new BadRequestAlertException("Invalid id, unable to find UserSettings for " + userDTO.getId(), ENTITY_NAME, "idnull");
        }

        // Update jhi_user entry
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("password", userDTO.getPassword());
        map.put("email", userDTO.getEmail());
        map.put("authorities", userDTO.getAuthorities());
        map.put("firstName", userDTO.getFirstName());
        map.put("lastName", userDTO.getLastName());

        ResponseEntity<String> response = oauth2ServiceClient.updateUser(map);
        if (response == null || !HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("User creation failed: " + response.getStatusCode().getReasonPhrase());
        }
        
        JSONObject obj = new JSONObject(response.getBody());
        System.out.println(obj);
        String lastModifiedBy = obj.getString("lastModifiedBy");
        Instant lastModifiedDate = Instant.parse(obj.getString("lastModifiedDate"));

        Boolean userSettingsModified = false;
        UserSettings existingUserSettings = existingUserSettingsOptional.get();
        Boolean existingMainContactFlag = existingUserSettings.getMainContact();
        if(existingMainContactFlag != userDTO.getMainContact()) {
            existingUserSettings.setMainContact(userDTO.getMainContact());
            userSettingsModified = true;
        }        
        
        // Check if salesforceId changed
        String existingSalesforceId = existingUserSettings.getSalesforceId();
        if(!existingSalesforceId.equals(userDTO.getSalesforceId())) {
            existingUserSettings.setSalesforceId(userDTO.getSalesforceId());
            userSettingsModified = true;
        }
        
        // Update UserSettings
        if(userSettingsModified) {
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
    @GetMapping("/user")
    public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws JSONException {
        log.debug("REST request to get a page of users");
        Page<UserSettings> page = userSettingsRepository.findAll(pageable);
        List<UserDTO> dtoList = new ArrayList<UserDTO>();

        for (UserSettings us : page) {
            UserDTO u = UserDTO.valueOf(us);
            ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(us.getLogin());
            JSONObject existingUser = new JSONObject(existingUserResponse.getBody());
            u.setEmail(existingUser.getString("email"));
            u.setFirstName(existingUser.getString("firstName"));
            u.setLastName(existingUser.getString("lastName"));
            dtoList.add(u);
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
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
        log.debug("REST request to get UserDTO : {}", id);
        Optional<UserSettings> msu = userSettingsRepository.findById(id);
        if (!msu.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // TODO: fetch User info from UAA and populate missing values in the
        // UserDTO
        return ResponseEntity.ok().body(UserDTO.valueOf(msu.get()));
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
