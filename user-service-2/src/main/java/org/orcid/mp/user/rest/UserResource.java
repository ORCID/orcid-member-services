package org.orcid.mp.user.rest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;


import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.rest.error.BadRequestAlertException;
import org.orcid.mp.user.rest.error.LoginAlreadyUsedException;
import org.orcid.mp.user.rest.validation.UserValidation;
import org.orcid.mp.user.rest.validation.UserValidator;
import org.orcid.mp.user.rest.vm.ResendActivationResponseVM;
import org.orcid.mp.user.security.AuthoritiesConstants;
import org.orcid.mp.user.security.SecurityUtils;
import org.orcid.mp.user.service.UserService;
import org.orcid.mp.user.upload.UserUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping
public class UserResource {

    private final Logger LOG = LoggerFactory.getLogger(UserResource.class);

    @Value("${application.name}")
    private String applicationName;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

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
        if (!userValidator.validate(userDTO, getCurrentUser().getLangKey()).isValid()) {
            return ResponseEntity.badRequest().body(userDTO);
        }

        Optional<UserDTO> updatedUser = userService.updateUser(userDTO);
        return updatedUser
                .map(user -> ResponseEntity.ok().body(user))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * {@code GET /users} : get all users.
     *
     * @param pageable    the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     * body all users.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(required = false, name = "filter") String filter, Pageable pageable) {
        Page<UserDTO> page;
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
        return ResponseEntity.ok(page);
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
            throw new BadRequestAlertException("Salesforce id doesn't match current user's memeber");
        }

        if (currentUser.getMainContact() == null || !currentUser.getMainContact().booleanValue()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UserDTO> users = userService.getAllUsersBySalesforceId(salesforceId);
        return ResponseEntity.ok(users);
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
    public ResponseEntity<Page<UserDTO>> getUsersBySalesforceId(@PathVariable String salesforceId, @RequestParam MultiValueMap<String, String> queryParams,
                                                                UriComponentsBuilder uriBuilder, @RequestParam(required = false, name = "filter") String filter,
                                                                Pageable pageable) {
        User currentUser = getCurrentUser();
        if (!currentUser.getSalesforceId().equals(salesforceId)) {
            throw new BadRequestAlertException("Salesforce id doesn't match current user's memeber");
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
        return ResponseEntity.ok(page);
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
     * @throws URISyntaxException
     */
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws URISyntaxException {
        LOG.debug("REST request to save UserDTO : {}", userDTO);
        if (!userValidator.validate(userDTO, getCurrentUser().getLangKey()).isValid()) {
            return ResponseEntity.badRequest().body(userDTO);
        }
        UserDTO user = userService.createUser(userDTO);
        return ResponseEntity.created(new URI("/api/users/" + user.getEmail())).body(user);
    }

    /**
     * {@code POST  /user} : Validate a memberServicesUser.
     *
     * @param userDTO: the user to validate.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and user
     * validation in the body.
     * @throws URISyntaxException
     */
    @PostMapping("/users/validate")
    public ResponseEntity<UserValidation> validateUser(@RequestBody UserDTO userDTO) throws URISyntaxException {
        Optional<User> currentUser = userRepository.findOneByEmailIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
        UserValidation validation = userValidator.validate(userDTO, currentUser.get().getLangKey());
        return ResponseEntity.ok(validation);
    }

    /**
     * {@code DELETE  /users/:login} : delete the 'login' user.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, @RequestParam(value = "noMainContactCheck", required = false) boolean noMainContactCheck) {
        LOG.debug("REST request to delete user {}", userId);
        String authUserLogin = SecurityUtils.getCurrentUserLogin().get();
        if (StringUtils.equalsIgnoreCase(authUserLogin, userId)) {
            throw new BadRequestAlertException("Cannot delete current authenticated user");
        }
        Optional<User> user = userService.getUser(userId);
        if (user.isPresent()) {
            // not main contact
            if (user.get().getMainContact() && !noMainContactCheck) {
                throw new BadRequestAlertException("Cannot delete main contact");
            }
            // not last admin
            if (user.get().getAdmin()) {
                // check if it is last admin
                if (userRepository.countByAdminIsTrue() == 1) {
                    throw new BadRequestAlertException("Cannot delete last admin");
                }
            }
        }
        userService.clearUser(userId);
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
            return ResponseEntity.ok().headers(JHipsterAlerts.createEntityUpdateAlert(applicationName, true, "user", salesforceId)).build();
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * {@code PUT /users/memberName/:oldMemberName/:newMemberName} : Updates memberName
     * for existing Users.
     *
     * @param salesforceId  the salesforceId for finding users to update
     * @param newMemberName the new Value of the memberName to update
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/users/memberName/{salesforceId}/{newMemberName}")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateUsersMemberName(@PathVariable String salesforceId, @PathVariable String newMemberName) {
        LOG.debug("REST request to update users' member names id to {}", newMemberName);
        boolean success = userService.updateUsersMemberName(salesforceId, newMemberName);
        if (success) {
            return ResponseEntity.ok().headers(JHipsterAlerts.createEntityUpdateAlert(applicationName, true, "user", salesforceId)).build();
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
