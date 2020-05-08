package org.orcid.auth.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.orcid.auth.config.Constants;
import org.orcid.auth.domain.Member;
import org.orcid.auth.domain.User;
import org.orcid.auth.repository.MemberRepository;
import org.orcid.auth.repository.UserRepository;
import org.orcid.auth.security.AuthoritiesConstants;
import org.orcid.auth.security.SecurityUtils;
import org.orcid.auth.service.MailService;
import org.orcid.auth.service.UserService;
import org.orcid.auth.service.dto.UserDTO;
import org.orcid.auth.upload.impl.MembersCsvReader;
import org.orcid.auth.web.rest.errors.BadRequestAlertException;
import org.orcid.auth.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.auth.web.rest.errors.LoginAlreadyUsedException;
import org.orcid.user.upload.MembersUpload;
import org.orcid.user.upload.UsersUpload;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
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
	private MemberRepository memberRepository;

	@Autowired
	private MailService mailService;

	/**
	 * {@code PUT /users} : Updates an existing User.
	 *
	 * @param userDTO the user to update.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is
	 *                                   already in use.
	 * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is
	 *                                   already in use.
	 */
	@PutMapping("/users")
	@PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
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
		Optional<UserDTO> updatedUser = userService.updateUser(userDTO);

		return ResponseUtil.wrapOrNotFound(updatedUser,
				HeaderUtil.createAlert(applicationName, "userManagement.updated", userDTO.getLogin()));
	}

	/**
	 * {@code GET /users} : get all users.
	 *
	 * @param queryParams a {@link MultiValueMap} query parameters.
	 * @param uriBuilder  a {@link UriComponentsBuilder} URI builder.
	 * @param pageable    the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         all users.
	 */
	@GetMapping("/users")
	public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam MultiValueMap<String, String> queryParams,
			UriComponentsBuilder uriBuilder, Pageable pageable) {
		final Page<UserDTO> page = userService.getAllManagedUsers(pageable);
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
	 * @param login the login of the user to find.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the "login" user, or with status {@code 404 (Not Found)}.
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

	/* FROM USER SETTINGS RESOURCE */

	/**
	 * {@code POST  /user/upload} : Create a list of users.
	 *
	 * @param file: file containing the users to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         a map indicating if each user was created or not, or with status
	 *         {@code 400 (Bad Request)} if the file cannot be parsed.
	 * @throws Throwable
	 */
	@PostMapping("/users/upload")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<String> uploadUsers(@RequestParam("file") MultipartFile file) throws Throwable {
		LOG.debug("Uploading users settings CSV");
		String createdBy = SecurityUtils.getAuthenticatedUser();
		UsersUpload upload = userService.uploadUserCSV(file.getInputStream(), createdBy);
		return ResponseEntity.ok().body(upload.getErrors().toString());
	}

	/**
	 * {@code POST  /user} : Create a new memberServicesUser.
	 *
	 * @param userDTO: the user to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         body the new user, or with status {@code 400 (Bad Request)} if the
	 *         user has already an ID.
	 * @throws JSONException
	 * @throws URISyntaxException
	 * @throws Throwable
	 */
	@PostMapping("/users")
	@PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
	public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException {
		LOG.debug("REST request to save UserDTO : {}", userDTO);
		if (!StringUtils.isBlank(userDTO.getId())) {
			throw new BadRequestAlertException("A new user cannot already have an ID", "User", "idexists");
		}

		if (!validate(userDTO)) {
			return ResponseEntity.badRequest().body(userDTO);
		}

		if (!memberRepository.existsBySalesforceId(userDTO.getSalesforceId())) {
			LOG.warn("Attempt to create user with non existent member {}", userDTO.getSalesforceId());
			return ResponseEntity.badRequest().body(userDTO);
		}

		String createdBy = SecurityUtils.getAuthenticatedUser();
		Instant now = Instant.now();
		userDTO.setCreatedBy(createdBy);
		userDTO.setCreatedDate(now);
		userDTO.setLastModifiedBy(createdBy);
		userDTO.setLastModifiedDate(now);

		User newUser = userService.createUser(userDTO);

		mailService.sendCreationEmail(newUser);
		return ResponseEntity.created(new URI("/api/users/" + newUser.getLogin()))
				.headers(HeaderUtil.createAlert(applicationName, "userManagement.created", newUser.getLogin()))
				.body(UserDTO.valueOf(newUser));
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
			user.setLoginError("Login name already used!");
		}

		existing = userRepository.findOneByEmailIgnoreCase(user.getEmail());
		if (existing.isPresent() && !existing.get().getDeleted()) {
			user.setEmailError("Email is already in use!");
		}
		return isOk;
	}

	/**
	 * {@code DELETE  /users/:login} : delete the 'login' user.
	 *
	 * @param login the id of the User to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 * @throws JSONException
	 */
	@DeleteMapping("/users/{jhiUserId}")
	public ResponseEntity<Void> deleteUser(@PathVariable String jhiUserId) {
		LOG.debug("REST request to delete user {}", jhiUserId);
		userService.clearUser(jhiUserId);
		return ResponseEntity.ok().headers(HeaderUtil.createAlert(applicationName, "userManagement.deleted", jhiUserId))
				.build();
	}

	/**
	 * {@code DELETE  /users/:id/:authority} : remove the authority from the given
	 * user.
	 *
	 * @param id        the id of the User.
	 * @param authority the authority to be removed
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@DeleteMapping("/users/{id}/{authority}")
	public ResponseEntity<Void> removeAuthority(@PathVariable String id, @PathVariable String authority) {
		LOG.debug("REST request to remove authority {} from user {}", authority, id);
		userService.removeAuthorityFromUser(id, authority);
		return ResponseEntity.accepted().build();
	}

	/* MEMBER SETTINGS - TO BE EXTRACTED */

	@Autowired
	private MembersCsvReader membersCsvReader;

	/**
	 * {@code POST  /member-settings} : Create a new memberSettings.
	 *
	 * @param memberSettings the memberSettings to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         body the new memberSettings, or with status {@code 400 (Bad Request)}
	 *         if the memberSettings has already an ID.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 * @throws JSONException
	 */
	@PostMapping("/members")
	public ResponseEntity<Member> createMember(@Valid @RequestBody Member memberSettings)
			throws URISyntaxException, JSONException {
		LOG.debug("REST request to save Member : {}", memberSettings);
		if (memberSettings.getId() != null) {
			throw new BadRequestAlertException("A new memberSettings cannot already have an ID", "Member", "idexists");
		}
		Optional<Member> optional = memberRepository.findBySalesforceId(memberSettings.getSalesforceId());
		// If user doesn't exists, create it
		if (optional.isPresent()) {
			throw new BadRequestAlertException("A member settings with that salesforce id already exists", "Member",
					"idexists");
		}
		if (!MemberValidator.validate(memberSettings)) {
			ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(applicationName, true, "Member",
					"memberSettings.create.error", memberSettings.getError()));
		}

		Instant now = Instant.now();
		memberSettings.setCreatedBy(SecurityUtils.getAuthenticatedUser());
		memberSettings.setCreatedDate(now);
		memberSettings.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
		memberSettings.setLastModifiedDate(now);

		Member result = memberRepository.save(memberSettings);
		return ResponseEntity
				.created(new URI("/api/member-settings/" + result.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, "Member", result.getId().toString()))
				.body(result);
	}

	/**
	 * {@code POST  /member-settings/upload} : Create a list of member settings.
	 *
	 * @param file: file containing the member-settings to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         a map indicating if each user was created or not, or with status
	 *         {@code 400 (Bad Request)} if the file cannot be parsed.
	 * @throws Throwable
	 */
	@PostMapping("/members/upload")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<String> uploadMember(@RequestParam("file") MultipartFile file) throws Throwable {
		LOG.debug("Uploading member settings CSV");
		MembersUpload upload = membersCsvReader.readMembersUpload(file.getInputStream());
		for (Member member : upload.getMembers()) {
			Instant now = Instant.now();
			Optional<Member> optional = memberRepository.findBySalesforceId(member.getSalesforceId());

			if (!optional.isPresent()) {
				member.setCreatedBy(SecurityUtils.getAuthenticatedUser());
				member.setCreatedDate(now);
				member.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
				member.setLastModifiedDate(now);
				memberRepository.save(member);
			} else {
				// If it exists, update it
				Member existingMember = optional.get();
				existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
				existingMember.setClientId(member.getClientId());
				existingMember.setIsConsortiumLead(member.getIsConsortiumLead());
				existingMember.setParentSalesforceId(member.getParentSalesforceId());
				member.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
				member.setLastModifiedDate(now);
				memberRepository.save(existingMember);
			}
		}
		return ResponseEntity.ok().body(upload.getErrors().toString());
	}

	/**
	 * {@code PUT  /member-settings} : Updates an existing memberSettings.
	 *
	 * @param memberSettings the memberSettings to update.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated memberSettings, or with status {@code 400 (Bad Request)}
	 *         if the memberSettings is not valid, or with status
	 *         {@code 500 (Internal Server Error)} if the memberSettings couldn't be
	 *         updated.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 * @throws JSONException
	 */
	@PutMapping("/members")
	public ResponseEntity<Member> updateMember(@Valid @RequestBody Member memberSettings)
			throws URISyntaxException, JSONException {
		LOG.debug("REST request to update Member : {}", memberSettings);
		if (memberSettings.getId() == null) {
			throw new BadRequestAlertException("Invalid id", "Member", "idnull");
		}
		Optional<Member> mso = memberRepository.findById(memberSettings.getId());
		if (!mso.isPresent()) {
			throw new BadRequestAlertException("Invalid id", "Member", "idunavailable");
		}
		if (!MemberValidator.validate(memberSettings)) {
			ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(applicationName, true, "Member",
					"memberSettings.create.error", memberSettings.getError()));
		}

		Instant now = Instant.now();
		memberSettings.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
		memberSettings.setLastModifiedDate(now);
		Member result = memberRepository.save(memberSettings);

		// Check if salesforceId changed
		Member existingMember = mso.get();

		if (!existingMember.getSalesforceId().equals(memberSettings.getSalesforceId())) {
			// If salesforceId changed, update each of the existing users with
			// the new salesforceId
			List<User> usList = userRepository.findBySalesforceId(existingMember.getSalesforceId());
			for (User us : usList) {
				us.setSalesforceId(memberSettings.getSalesforceId());
				us.setLastModifiedBy(SecurityUtils.getAuthenticatedUser());
				us.setLastModifiedDate(now);
				userRepository.save(us);
			}
		}

		return ResponseEntity.ok().headers(
				HeaderUtil.createEntityUpdateAlert(applicationName, true, "Member", memberSettings.getId().toString()))
				.body(result);
	}

	/**
	 * {@code GET  /member-settings} : get all the memberSettings.
	 *
	 *
	 * @param pageable the pagination information.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
	 *         of memberSettings in body.
	 */
	@GetMapping("/members")
	public ResponseEntity<List<Member>> getAllMember(Pageable pageable) {
		LOG.debug("REST request to get a page of Member");
		Page<Member> page = memberRepository.findAll(pageable);
		HttpHeaders headers = PaginationUtil
				.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
		return ResponseEntity.ok().headers(headers).body(page.getContent());
	}

	/**
	 * {@code GET  /member-settings/:id} : get the "id" memberSettings.
	 *
	 * @param id - the id or salesforce id of the memberSettings to retrieve.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the memberSettings, or with status {@code 404 (Not Found)}.
	 */
	@GetMapping("/members/{id}")
	public ResponseEntity<Member> getMember(@PathVariable String id) {
		LOG.debug("REST request to get Member : {}", id);
		Optional<Member> memberSettings = memberRepository.findById(id);
		if (!memberSettings.isPresent()) {
			LOG.debug("Member settings now found for id {}, searching against salesforceId", id);
			memberSettings = memberRepository.findBySalesforceId(id);
		}
		return ResponseUtil.wrapOrNotFound(memberSettings);
	}

	/**
	 * {@code DELETE  /member-settings/:id} : delete the "id" memberSettings.
	 *
	 * @param id the id of the memberSettings to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@DeleteMapping("/members/{id}")
	public ResponseEntity<Void> deleteMember(@PathVariable String id) {
		LOG.debug("REST request to delete Member : {}", id);

		// Can't delete a memberSettings object if there is at least one
		// userSettings linked to it
		Optional<Member> mso = memberRepository.findById(id);
		if (!mso.isPresent()) {
			throw new BadRequestAlertException("Invalid id", "Member", "idunavailable");
		}

		// If there is at least one userSettings assigned to this
		// memberSettings, throw an exception
		userRepository.findBySalesforceId(id).stream().map(user -> {
			throw new BadRequestAlertException("Unable to delete Member, user '" + user.getId() + "' still use it",
					"Member", "idused");
		});

		memberRepository.deleteById(id);
		return ResponseEntity.noContent()
				.headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "Member", id)).build();
	}

}
