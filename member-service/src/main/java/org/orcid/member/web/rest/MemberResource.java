package org.orcid.member.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.codehaus.jettison.json.JSONException;
import org.orcid.member.domain.Member;
import org.orcid.member.service.MemberService;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * REST controller for managing members.
 * <p>
 * This class accesses the {@link Member} entity, and needs to fetch its
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
public class MemberResource {

	private final Logger LOG = LoggerFactory.getLogger(MemberResource.class);

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	@Autowired
	private MemberService memberService;

	/**
	 * {@code POST  /member} : Create a new member.
	 *
	 * @param member the member to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         body the new member, or with status {@code 400 (Bad Request)} if the
	 *         member has already an ID.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 * @throws JSONException
	 */
	@PostMapping("/members")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<Member> createMember(@Valid @RequestBody Member member)
			throws URISyntaxException, JSONException {
		LOG.debug("REST request to save Member : {}", member);
		Member created = memberService.createMember(member);
		return ResponseEntity
				.created(new URI("/api/member/" + created.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, "member", created.getId().toString()))
				.body(created);
	}

	/**
	 * {@code POST  /members/upload} : Create a list of member settings.
	 *
	 * @param file: file containing the member to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         a map indicating if each user was created or not, or with status
	 *         {@code 400 (Bad Request)} if the file cannot be parsed.
	 * @throws Throwable
	 */
	@PostMapping("/members/upload")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<String> uploadMember(@RequestParam("file") MultipartFile file) throws Throwable {
		LOG.debug("Uploading member CSV");
		MemberUpload upload = memberService.uploadMemberCSV(file.getInputStream());
		return ResponseEntity.ok().body(upload.getErrors().toString());
	}

	/**
	 * {@code PUT  /members} : Updates an existing member.
	 *
	 * @param member the member to update.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated member, or with status {@code 400 (Bad Request)} if the
	 *         member is not valid, or with status
	 *         {@code 500 (Internal Server Error)} if the member couldn't be
	 *         updated.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 * @throws JSONException
	 */
	@PutMapping("/members")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<Member> updateMember(@Valid @RequestBody Member member)
			throws URISyntaxException, JSONException {
		LOG.debug("REST request to update Member : {}", member);
		Optional<Member> existentMember = memberService.getMember(member.getId());
		if (!existentMember.isPresent()) {
			throw new BadRequestAlertException("Invalid id", "member", "idunavailable");
		}
		member = memberService.updateMember(member);
		memberService.updateUsersOnConsortiumLeadChange(member, existentMember.get());
		return ResponseEntity.ok()
				.headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "member", member.getId().toString()))
				.body(member);
	}

	/**
	 * {@code GET  /member} : get all the member.
	 *
	 *
	 * @param pageable the pagination information.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
	 *         of member in body.
	 */
	@GetMapping("/members")
	public ResponseEntity<List<Member>> getAllMembers(Pageable pageable) {
		LOG.debug("REST request to get a page of Member");
		Page<Member> page = memberService.getAllMembers(pageable);
		HttpHeaders headers = PaginationUtil
				.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
		return ResponseEntity.ok().headers(headers).body(page.getContent());
	}

	/**
	 * {@code GET  /members/:id} : get the "id" member.
	 *
	 * @param id - the id or salesforce id of the member to retrieve.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the member, or with status {@code 404 (Not Found)}.
	 */
	@GetMapping("/members/{id}")
	public ResponseEntity<Member> getMember(@PathVariable String id) {
		LOG.debug("REST request to get Member : {}", id);
		Optional<Member> member = memberService.getMember(id);
		return ResponseUtil.wrapOrNotFound(member);
	}

	/**
	 * {@code GET  /members/authorized/:encryptedEmail} : get the authorized member
	 * details for the specified encrypted email.
	 *
	 * @param encryptedEmail - the encrypted email of the user that has authorized
	 *                       the member
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with the
	 *         member details in the body, or status {@code 404 (Not Found)}.
	 */
	@GetMapping("/members/authorized/{encryptedEmail}")
	public ResponseEntity<Member> getAuthorizedMember(@PathVariable String encryptedEmail) {
		LOG.debug("REST request to get authorized Member details for encrypted email : {}", encryptedEmail);
		Optional<Member> member = memberService.getAuthorizedMemberForUser(encryptedEmail);
		return ResponseUtil.wrapOrNotFound(member);
	}

	/**
	 * {@code DELETE  /members/:id} : delete the "id" member.
	 *
	 * @param id the id of the member to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@DeleteMapping("/members/{id}")
	@PreAuthorize("hasRole(\"ROLE_ADMIN\")")
	public ResponseEntity<Void> deleteMember(@PathVariable String id) {
		LOG.debug("REST request to delete Member : {}", id);
		memberService.deleteMember(id);
		return ResponseEntity.noContent()
				.headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "member", id)).build();
	}

}
