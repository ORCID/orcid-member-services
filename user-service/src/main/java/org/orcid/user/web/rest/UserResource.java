package org.orcid.user.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
import org.orcid.user.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.user.web.rest.errors.LoginAlreadyUsedException;
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

    /**
     * {@code PUT /users} : Updates an existing User.
     *
     * @param userDTO
     *            the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the updated user.
     * @throws EmailAlreadyUsedException
     *             {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException
     *             {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping("/users")
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UserDTO userDTO) {
        LOG.debug("REST request to update User : {}", userDTO);
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDTO.getId()))) {
            throw new EmailAlreadyUsedException();
        }
        existingUser = userRepository.findOneByLogin(userDTO.getLogin().toLowerCase());
        // XXX - eh?
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDTO.getId()))) {
            throw new LoginAlreadyUsedException();
        }
        //change the auth if the logged in user is org owner and this is set as mainContact
        Optional<User> authUser = userRepository.findOneByLogin(SecurityUtils.getAuthenticatedUser());
        if(userDTO.getMainContact()) 
        {   

            existingUser = userRepository.findOneByMainContactIsTrueAndSalesforceId(userDTO.getSalesforceId());
            
            if(existingUser.isPresent()) {
                if(SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ORG_OWNER) ){
                    if(!StringUtils.equals(authUser.get().getId(), userDTO.getId())) {
                        userService.removeOwnershipFromUser(authUser.get().getLogin());
                    }
                    userDTO.getAuthorities().add(AuthoritiesConstants.ORG_OWNER);
                }
                else if(!StringUtils.equals(existingUser.get().getId(), userDTO.getId())) {
                    throw new BadRequestAlertException("Owner already exists for organization " + userDTO.getSalesforceId(), "user", "ownerExists");
                }
            }
        }
        
        Optional<UserDTO> updatedUser = userService.updateUser(userDTO);

        return ResponseUtil.wrapOrNotFound(updatedUser, HeaderUtil.createEntityUpdateAlert(applicationName, true, "user", userDTO.getLogin()));
    }

    /**
     * {@code GET /users} : get all users.
     *
     * @param queryParams
     *            a {@link MultiValueMap} query parameters.
     * @param uriBuilder
     *            a {@link UriComponentsBuilder} URI builder.
     * @param pageable
     *            the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body all users.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder, Pageable pageable) {
        final Page<UserDTO> page = userService.getAllManagedUsers(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * {@code GET /users/salesforce/:salesforceId} : get users by salesforce id.
     *
     * @param queryParams
     *            a {@link MultiValueMap} query parameters.
     * @param uriBuilder
     *            a {@link UriComponentsBuilder} URI builder.
     * @param pageable
     *            the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body all users.
     */
    @GetMapping("/users/salesforce/{salesforceId}")
    public ResponseEntity<List<UserDTO>> getUsersBySalesforceId(@PathVariable String salesforceId) {
        List<UserDTO> users = userService.getAllUsersBySalesforceId(salesforceId);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * {@code GET /users/salesforce/:salesforceId/p} : get users by salesforce
     * id.
     * 
     * @param salesforceId
     *            the salesforce id for the organization.
     * @param queryParams
     *            a {@link MultiValueMap} query parameters.
     * @param uriBuilder
     *            a {@link UriComponentsBuilder} URI builder.
     * @param pageable
     *            the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body all users.
     */
    @GetMapping("/users/salesforce/{salesforceId}/p")
    public ResponseEntity<List<UserDTO>> getUsersBySalesforceId(@PathVariable String salesforceId, @RequestParam MultiValueMap<String, String> queryParams,
            UriComponentsBuilder uriBuilder, Pageable pageable) {
        final Page<UserDTO> page = userService.getAllUsersBySalesforceId(pageable, salesforceId);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * Gets a list of all roles.
     *
     * @return a string list of all roles.
     */
    @GetMapping("/users/authorities")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public List<String> getAuthorities() {
        return userService.getAuthorities();
    }

    /**
     * {@code GET /users/:login} : get the "login" user.
     *
     * @param login
     *            the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{loginOrId}")
    public ResponseEntity<UserDTO> getUserByIdOrLogin(@PathVariable String loginOrId) {
        LOG.debug("REST request to get User : {}", loginOrId);
        Optional<User> user = userService.getUserWithAuthoritiesByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUserWithAuthorities(loginOrId);
        }
        return ResponseUtil.wrapOrNotFound(user.map(UserDTO::valueOf));
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
    @PostMapping("/users/upload")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<String> uploadUsers(@RequestParam("file") MultipartFile file) throws Throwable {
        LOG.debug("Uploading users settings CSV");
        String createdBy = SecurityUtils.getAuthenticatedUser();
        UserUpload upload = userService.uploadUserCSV(file.getInputStream(), createdBy);
        return ResponseEntity.ok().body(upload.getErrors().toString());
    }

    /**
     * {@code POST  /user} : Create a new memberServicesUser.
     *
     * @param userDTO:
     *            the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with body the new user, or with status {@code 400 (Bad Request)}
     *         if the user has already an ID.
     * @throws JSONException
     * @throws URISyntaxException
     * @throws Throwable
     */
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException {
        LOG.debug("REST request to save UserDTO : {}", userDTO);
        if (!StringUtils.isBlank(userDTO.getId())) {
            throw new BadRequestAlertException("A new user cannot already have an ID", "User", "idexists");
        }
        if (!validate(userDTO)) {
            return ResponseEntity.badRequest().body(userDTO);
        }
        if (!userService.memberExists(userDTO.getSalesforceId())) {
            LOG.warn("Attempt to create user with non existent member {}", userDTO.getSalesforceId());
            return ResponseEntity.badRequest().body(userDTO);
        }
        String createdBy = SecurityUtils.getAuthenticatedUser();
        //change the auth if the logged in user is org owner and this is set as mainContact
        if(userDTO.getMainContact()) 
        {
            if(SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ORG_OWNER)) {
                userService.removeOwnershipFromUser(createdBy);
            }
            userDTO.getAuthorities().add(AuthoritiesConstants.ORG_OWNER);
        }
        
        Instant now = Instant.now();
        userDTO.setCreatedBy(createdBy);
        userDTO.setCreatedDate(now);
        userDTO.setLastModifiedBy(createdBy);
        userDTO.setLastModifiedDate(now);

        User newUser = userService.createUser(userDTO);
        return ResponseEntity.created(new URI("/api/users/" + newUser.getLogin()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "user", newUser.getLogin())).body(UserDTO.valueOf(newUser));

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
            LOG.info("Salesforce id missing");
        }

        Optional<User> existing = userRepository.findOneByLogin(user.getLogin().toLowerCase());
        if (existing.isPresent() && !existing.get().getDeleted()) {
            throw new BadRequestAlertException("Invalid email", "user", "emailUsed");
        }

        existing = userRepository.findOneByEmailIgnoreCase(user.getEmail());
        if (existing.isPresent() && !existing.get().getDeleted()) {
            throw new BadRequestAlertException("Invalid email", "user", "emailUsed");
        }

        if (new EmailValidator().isValid(user.getEmail(), null)) {
            user.setEmailError("Email is invalid!");
        }
        
      //change the auth if the logged in user is org owner and this is set as mainContact
        Optional<User> authUser = userRepository.findOneByLogin(SecurityUtils.getAuthenticatedUser());
        if(authUser.isPresent() && !StringUtils.equals(authUser.get().getId(), user.getId()) && user.getMainContact() && SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ORG_OWNER) ) 
        {
            userService.removeAuthorityFromUser(authUser.get().getId(), AuthoritiesConstants.ORG_OWNER);
        }
        else {
            existing = userRepository.findOneByMainContactIsTrueAndSalesforceId(user.getSalesforceId());
            if(existing.isPresent()) {
                throw new BadRequestAlertException("Owner already exists for organization " + user.getSalesforceId(), "user", "ownerExists");
            }
        }
        return isOk;
    }

    /**
     * {@code DELETE  /users/:login} : delete the 'login' user.
     *
     * @param login
     *            the id of the User to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     * @throws JSONException
     */
    @DeleteMapping("/users/{jhiUserId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String jhiUserId) {
        LOG.debug("REST request to delete user {}", jhiUserId);
        userService.clearUser(jhiUserId);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "user", jhiUserId)).build();
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
     */
    @DeleteMapping("/users/{id}/{authority}")
    public ResponseEntity<Void> removeAuthority(@PathVariable String id, @PathVariable String authority) {
        LOG.debug("REST request to remove authority {} from user {}", authority, id);
        userService.removeAuthorityFromUser(id, authority);
        return ResponseEntity.accepted().build();
    }

    /**
     * {@code PUT /users/:id/sendActivate} : send the activation email.
     *
     * @param login
     *            the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @PutMapping("/users/{loginOrId}/sendActivate")
    public ResponseEntity<UserDTO> sendActivate(@PathVariable String loginOrId) {
        LOG.debug("REST request to get User : {}", loginOrId);
        Optional<User> user = userService.getUserWithAuthoritiesByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUserWithAuthorities(loginOrId);
        }
    
        userService.sendActivationEmail(user.get().getEmail());
        return ResponseUtil.wrapOrNotFound(user.map(UserDTO::valueOf));
    }

    /**
     * {@code PUT /users/:salesforceId/:newSalesforceId} : Updates salesForceId
     * for existing Users.
     *
     * @param salesforceId
     *            the salesforceId to the find the users to update.
     * @param newSalesforceId
     *            the new salesforceId to update.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/users/{salesforceId}/{newSalesforceId}")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateUserSalesforceOrAssertion(@PathVariable String salesforceId, @PathVariable String newSalesforceId) {
        LOG.debug("REST request to update Users by salesforce : {}", salesforceId);
        List<UserDTO> usersBelongingToMember = userService.getAllUsersBySalesforceId(salesforceId);
        for (UserDTO user : usersBelongingToMember) {
            user.setSalesforceId(newSalesforceId);
            userService.updateUser(user);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "user", salesforceId)).build();
    }

}
