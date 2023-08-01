package org.orcid.memberportal.service.user.web.rest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.dto.UserDTO;
import org.orcid.memberportal.service.user.mapper.UserMapper;
import org.orcid.memberportal.service.user.repository.UserRepository;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.security.SecurityUtils;
import org.orcid.memberportal.service.user.services.MailService;
import org.orcid.memberportal.service.user.services.MemberService;
import org.orcid.memberportal.service.user.services.UserService;
import org.orcid.memberportal.service.user.upload.UserUpload;
import org.orcid.memberportal.service.user.validation.UserValidation;
import org.orcid.memberportal.service.user.validation.UserValidator;
import org.orcid.memberportal.service.user.web.rest.errors.BadRequestAlertException;
import org.orcid.memberportal.service.user.web.rest.errors.LoginAlreadyUsedException;
import org.orcid.memberportal.service.user.web.rest.vm.ResendActivationResponseVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing users.
 * <p>
 * This class accesses the {@link User} entity, and needs to fetch its
 * collection of authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship
 * between User and Authority, and send everything to the client side: there
 * would be no View Model and DTO, a lot less code, and an outer-join which
 * would be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities,
 * because people will quite often do relationships with the user, and we don't
 * want them to get the authorities all the time for nothing (for performance
 * reasons). This is the #1 goal: we should not impact our users' application
 * because of this use-case.</li>
 * <li>Not having an outer join causes n+1 requests to the database. This is not
 * a real issue as we have by default a second-level cache. This means on the
 * first HTTP call we do the n+1 requests, but then all authorities come from
 * the cache, so in fact it's much better than doing an outer join (which will
 * get lots of data from the database, for each HTTP call).</li>
 * <li>As this manages users, for security reasons, we'd rather have a DTO
 * layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this
 * case.
 */
@RestController
@RequestMapping("/api")
public class UserResource {

    private final Logger LOG = LoggerFactory.getLogger(UserResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private UserMapper userMapper;

    /**
     * {@code PUT /users} : Updates an existing User.
     *
     * @param userDTO the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body the updated user.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping("/users")
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UserDTO userDTO) {
        LOG.debug("REST request to update User : {}", userDTO);
        if (!userValidator.validate(userDTO, getCurrentUser()).isValid()) {
            return ResponseEntity.badRequest().body(userDTO);
        }

        Optional<UserDTO> updatedUser = userService.updateUser(userDTO);
        return ResponseUtil.wrapOrNotFound(updatedUser);
    }

    /**
     * {@code GET /users} : get all users.
     *
     * @param queryParams a {@link MultiValueMap} query parameters.
     * @param uriBuilder  a {@link UriComponentsBuilder} URI builder.
     * @param pageable    the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body all users.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam MultiValueMap<String, String> queryParams,
                                                     @RequestParam(required = false, name = "filter") String filter, UriComponentsBuilder uriBuilder, Pageable pageable) {
        Page<UserDTO> page = null;
        if (StringUtils.isBlank(filter)) {
            page = userService.getAllManagedUsers(pageable);
        } else {
            String decodedFilter;
            try {
                decodedFilter = URLDecoder.decode(filter, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                /* try without decoding if this ever happens */
                decodedFilter = filter;
            }
            page = userService.getAllManagedUsers(pageable, decodedFilter);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * {@code GET /users/salesforce/:salesforceId} : get users by salesforce id.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body all users.
     */
    @GetMapping("/users/salesforce/{salesforceId}")
    public ResponseEntity<List<UserDTO>> getUsersBySalesforceId(@PathVariable String salesforceId) {
        User currentUser = getCurrentUser();
        if (!currentUser.getSalesforceId().equals(salesforceId)) {
            throw new BadRequestAlertException("Salesforce id doesn't match current user's memeber", "User", "badSalesforceId");
        }

        if (currentUser.getMainContact() == null || !currentUser.getMainContact().booleanValue()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UserDTO> users = userService.getAllUsersBySalesforceId(salesforceId);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * {@code GET /users/salesforce/:salesforceId/p} : get users by salesforce
     * id.
     *
     * @param salesforceId the salesforce id for the organization.
     * @param queryParams  a {@link MultiValueMap} query parameters.
     * @param uriBuilder   a {@link UriComponentsBuilder} URI builder.
     * @param pageable     the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body all users.
     */
    @GetMapping("/users/salesforce/{salesforceId}/p")
    public ResponseEntity<List<UserDTO>> getUsersBySalesforceId(@PathVariable String salesforceId, @RequestParam MultiValueMap<String, String> queryParams,
                                                                UriComponentsBuilder uriBuilder, @RequestParam(required = false, name = "filter") String filter,
                                                                Pageable pageable) {
        User currentUser = getCurrentUser();
        if (!currentUser.getSalesforceId().equals(salesforceId)) {
            throw new BadRequestAlertException("Salesforce id doesn't match current user's memeber", "User", "badSalesforceId");
        }

        if (currentUser.getMainContact() == null || !currentUser.getMainContact().booleanValue()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Page<UserDTO> page = null;
        if (StringUtils.isBlank(filter)) {
            page = userService.getAllUsersBySalesforceId(pageable, salesforceId);
        } else {
            String decodedFilter;
            try {
                decodedFilter = URLDecoder.decode(filter, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                /* try without decoding if this ever happens */
                decodedFilter = filter;
            }
            page = userService.getAllUsersBySalesforceId(pageable, salesforceId, decodedFilter);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * {@code GET /users/:login} : get the "login" user.
     *
     * @param loginOrId the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{loginOrId}")
    public ResponseEntity<UserDTO> getUserByLogin(@PathVariable String loginOrId) {
        LOG.debug("REST request to get User : {}", loginOrId);
        Optional<User> user = userService.getUserByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUser(loginOrId);
        }
        return userOrNotFound(user);
    }

    /**
     * {@code POST  /user/upload} : Create a list of users.
     *
     * @param file: file containing the users to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     * with a map indicating if each user was created or not, or with
     * status {@code 400 (Bad Request)} if the file cannot be parsed.
     * @throws Throwable
     */
    @PostMapping("/users/upload")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<String> uploadUsers(@RequestParam("file") MultipartFile file) throws Throwable {
        LOG.debug("Uploading users settings CSV");
        User currentUser = getCurrentUser();
        UserUpload upload = userService.uploadUserCSV(file.getInputStream(), currentUser);
        return ResponseEntity.ok().body(upload.getErrors().toString());
    }

    /**
     * {@code POST  /user} : Create a new memberServicesUser.
     *
     * @param userDTO: the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     * with body the new user, or with status {@code 400 (Bad Request)}
     * if the user has already an ID.
     * @throws JSONException
     * @throws URISyntaxException
     * @throws Throwable
     */
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws URISyntaxException {
        LOG.debug("REST request to save UserDTO : {}", userDTO);
        if (!StringUtils.isBlank(userDTO.getId())) {
            throw new BadRequestAlertException("A new user cannot already have an ID", "User", "idexists");
        }
        if (!userValidator.validate(userDTO, getCurrentUser()).isValid()) {
            return ResponseEntity.badRequest().body(userDTO);
        }
        if (!userService.memberExists(userDTO.getSalesforceId())) {
            LOG.warn("Attempt to create user with non existent member {}", userDTO.getSalesforceId());
            return ResponseEntity.badRequest().body(userDTO);
        }
        String createdBy = SecurityUtils.getCurrentUserLogin().get();
        // change the auth if the logged in user is org owner and this is set as
        // mainContact
        boolean owner = userDTO.getMainContact();
        List<User> owners = userRepository.findAllByMainContactIsTrueAndDeletedIsFalseAndSalesforceId(userDTO.getSalesforceId());

        if (owner) {
            for (User prevOwner : owners) {
                if (!StringUtils.equals(prevOwner.getId(), userDTO.getId())) {
                    userService.removeOwnershipFromUser(prevOwner.getEmail());
                }
            }
            userDTO.getAuthorities().add(AuthoritiesConstants.ORG_OWNER);
        } else {
            if (owners.isEmpty()) {
                userDTO.setMainContact(true);
                userDTO.getAuthorities().add(AuthoritiesConstants.ORG_OWNER);
            }
        }

        Instant now = Instant.now();
        userDTO.setCreatedBy(createdBy);
        userDTO.setCreatedDate(now);
        userDTO.setLastModifiedBy(createdBy);
        userDTO.setLastModifiedDate(now);

        User newUser = userService.createUser(userDTO);

        if (owner) {
            String member = memberService.getMemberNameBySalesforce(newUser.getSalesforceId());
            mailService.sendOrganizationOwnerChangedMail(newUser, member);
        }

        return ResponseEntity.created(new URI("/api/users/" + newUser.getEmail())).body(userMapper.toUserDTO(newUser));
    }

    /**
     * {@code POST  /user} : Validate a memberServicesUser.
     *
     * @param userDTO: the user to validate.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and user
     * validation in the body.
     * @throws JSONException
     * @throws URISyntaxException
     * @throws Throwable
     */
    @PostMapping("/users/validate")
    public ResponseEntity<UserValidation> validateUser(@RequestBody UserDTO userDTO) throws URISyntaxException {
        Optional<User> currentUser = userRepository.findOneByEmailIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
        UserValidation validation = userValidator.validate(userDTO, currentUser.get());
        return ResponseEntity.ok(validation);
    }

    /**
     * {@code DELETE  /users/:login} : delete the 'login' user.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     * @throws JSONException
     */
    @DeleteMapping("/users/{jhiUserId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String jhiUserId, @RequestParam(value = "noMainContactCheck", required = false) boolean noMainContactCheck) {
        LOG.debug("REST request to delete user {}", jhiUserId);
        String authUserLogin = SecurityUtils.getCurrentUserLogin().get();
        if (StringUtils.equalsIgnoreCase(authUserLogin, jhiUserId)) {
            throw new BadRequestAlertException("Cannot delete current authenticated user", "User", "delete.auth.user.string");
        }
        Optional<User> user = userService.getUser(jhiUserId);
        if (user.isPresent()) {
            // not main contact
            if (user.get().getMainContact() && !noMainContactCheck) {
                throw new BadRequestAlertException("Cannot delete main contact", "User", "delete.main.contact.string");
            }
            // not last admin
            if (user.get().getAdmin()) {
                // check if it is last admin
                if (userRepository.countByAdminIsTrue() == 1) {
                    throw new BadRequestAlertException("Cannot delete last admin", "User", "delete.last.admin.string");
                }
            }
        }
        userService.clearUser(jhiUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code POST /users/:id/sendActivate} : send the activation email.
     *
     * @param loginOrId the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @PostMapping("/users/{loginOrId}/sendActivate")
    public ResponseEntity<UserDTO> sendActivate(@PathVariable String loginOrId) {
        LOG.debug("REST request to send user activation: {}", loginOrId);
        Optional<User> user = userService.getUserByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUser(loginOrId);
        }

        userService.sendActivationEmail(user.get().getEmail());
        return userOrNotFound(user);
    }

    /**
     * {@code POST /users/:id/resendActivate} : send the activation email.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @PostMapping("/users/{key}/resendActivation")
    public ResponseEntity<ResendActivationResponseVM> resendActivation(@PathVariable String key) {
        LOG.debug("REST request to resend user activation for key : {}", key);
        ResendActivationResponseVM response = new ResendActivationResponseVM();
        try {
            userService.resendActivationEmail(key);
            response.setResent(true);
        } catch (Exception e) {
            response.setResent(false);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * {@code PUT /users/:salesforceId/:newSalesforceId} : Updates salesForceId
     * for existing Users.
     *
     * @param salesforceId    the salesforceId to the find the users to update.
     * @param newSalesforceId the new salesforceId to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/users/{salesforceId}/{newSalesforceId}")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateUsersSalesforceId(@PathVariable String salesforceId, @PathVariable String newSalesforceId) {
        LOG.debug("REST request to update users' salesforce id from {} to {}", salesforceId, newSalesforceId);
        boolean success = userService.updateUsersSalesforceId(salesforceId, newSalesforceId);
        if (success) {
            return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "user", salesforceId)).build();
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * {@code GET /users/:saleforceId}/owner : get the "login" user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{salesforceId}/owner")
    public boolean getOwner(@PathVariable String salesforceId) {
        LOG.debug("REST request to get Owner for : {}", salesforceId);
        return userService.hasOwnerForSalesforceId(salesforceId);
    }

    /**
     * {@code POST /switch_user} : Switch user
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/switch_user")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> switchUser(@RequestParam(value = "username", required = true) String username) {
        User user = userService.getCurrentUser();
        UserDTO userDTO = userMapper.toUserDTO(user);
        userDTO.setLoginAs(username);
        userDTO.setIsAdmin(true);
        userService.updateUser(userDTO);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build();
    }

    /**
     * {@code POST /logout_as} : Switch user
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/logout_as")
    public ResponseEntity<Void> logoutAsSwitchedUser(@RequestParam(value = "username", required = true) String username) {
        Optional<User> authUser = userService.getUserByLogin(SecurityUtils.getCurrentUserLogin().get());
        if (authUser.isPresent()) {
            UserDTO userDTO = userMapper.toUserDTO(authUser.get());
            userDTO.setIsAdmin(true);
            userDTO.setLoginAs(null);
            userService.updateUser(userDTO);
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build();
    }

    private User getCurrentUser() {
        return userRepository.findOneByEmailIgnoreCase(SecurityUtils.getCurrentUserLogin().get()).get();
    }

    private ResponseEntity<UserDTO> userOrNotFound(Optional<User> user) {
        if (!user.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userMapper.toUserDTO(user.get()));
    }
}
