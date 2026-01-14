package org.orcid.mp.member.rest;

import io.micrometer.common.util.StringUtils;
import org.codehaus.jettison.json.JSONException;

import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.MemberContactUpdateResponse;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.rest.validation.MemberValidation;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.upload.MemberUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping
public class MemberResource {

    private final Logger LOG = LoggerFactory.getLogger(MemberResource.class);

    @Autowired
    private MemberService memberService;

    /**
     * {@code POST  /members} : Create a new member.
     *
     * @param member the member to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with body the new member, or with status
     *         {@code 400 (Bad Request)} if the member has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     * @throws JSONException
     */
    @PostMapping("/members")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<Member> createMember(@Valid @RequestBody Member member)
            throws URISyntaxException, JSONException {
        LOG.debug("REST request to save Member : {}", member);
        Member created = memberService.createMember(member);
        return ResponseEntity.created(new URI("/api/member/" + created.getId())).body(created);
    }

    /**
     * {@code POST  /members/validate} : Validates a member.
     *
     * @param member the member to validate.
     * @return the {@link ResponseEntity} with status {@code 200 (Ok)} and with
     *         a MemberValidation object in the body.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     * @throws JSONException
     */
    @PostMapping("/members/validate")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<MemberValidation> validateMember(@Valid @RequestBody Member member)
            throws URISyntaxException, JSONException {
        MemberValidation validation = memberService.validateMember(member);
        return ResponseEntity.ok(validation);
    }

    /**
     * {@code POST  /members/upload} : Create a list of member settings.
     *
     * @param file: file containing the member to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and
     *         with a map indicating if each user was created or not, or with
     *         status {@code 400 (Bad Request)} if the file cannot be parsed.
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
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the updated member, or with status {@code 400 (Bad Request)}
     *         if the member is not valid, or with status
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
            throw new BadRequestAlertException("Invalid id");
        }
        member = memberService.updateMember(member);
        return ResponseEntity.ok().body(member);
    }

    /**
     * {@code POST  /members/:id/language/:language } : Updates an existing member's
     * default language.
     *
     * @param salesforceId - the salesforceId of the member to update
     * @param language     - the language of the member to update
     * @return the {@link ResponseEntity} with status {@code 200 (OK)},
     *         or with status {@code 400 (Bad Request)}
     *         if the member is not valid, or with status
     *         {@code 500 (Internal Server Error)} if the member couldn't be
     *         updated.
     */
    @PostMapping("/members/{salesforceId}/language/{language}")
    public ResponseEntity<Void> updateMemberDefaultLanguage(@PathVariable String salesforceId,
            @PathVariable String language) {
        LOG.info("REST request to update default language for member : {}", salesforceId);
        try {
            memberService.updateMemberDefaultLanguage(salesforceId, language);
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * {@code PUT /members/{salesforceId}/member-details} : update details of member
     * specified by salesforceID
     *
     * @return the {@link MemberUpdateData}
     */
    @PutMapping("/members/{salesforceId}/member-details")
    public ResponseEntity<Boolean> updatePublicMemberDetails(@RequestBody MemberUpdateData memberUpdateData,
            @PathVariable String salesforceId) {
        LOG.info("REST request to update member public details for salesforce id {}", salesforceId);
        if (!memberDetailsUpdateValid(memberUpdateData)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            boolean success = memberService.updateMemberData(memberUpdateData, salesforceId);
            return ResponseEntity.ok(success);
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * {@code GET /members/{salesforceId}/member-details} : get details of member
     * specified by salesforceId param
     *
     * @return the {@link MemberDetails}
     */
    @GetMapping("/members/{salesforceId}/member-details")
    public ResponseEntity<MemberDetails> getMemberDetails(@PathVariable String salesforceId) {
        LOG.debug("REST request to get member details");
        try {
            MemberDetails memberDetails = memberService.getMemberDetails(salesforceId);
            return ResponseEntity.ok(memberDetails);
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<List<Country>> getSalesforceCountries() {
        LOG.debug("REST request to get salesforce countries");
        List<Country> countries = memberService.getSalesforceCountries();
        return ResponseEntity.ok(countries);
    }

    /**
     * {@code GET /members/{salesforceId}/member-contacts} : get contacts of member
     * specified by salesforceId param
     *
     * @return the {@link MemberDetails}
     */
    @GetMapping("/members/{salesforceId}/member-contacts")
    public ResponseEntity<MemberContacts> getMemberContacts(@PathVariable String salesforceId) {
        LOG.debug("REST request to get member contacts for member {}", salesforceId);
        try {
            MemberContacts memberContacts = memberService.getCurrentMemberContacts(salesforceId);
            return ResponseEntity.ok(memberContacts);
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * {@code GET /members/{salesforceId}/member-org-ids} : get org ids of member
     * specified by salesforceId param
     *
     * @return the {@link MemberDetails}
     */
    @GetMapping("/members/{salesforceId}/member-org-ids")
    public ResponseEntity<MemberOrgIds> getMemberOrgIds(@PathVariable String salesforceId) {
        LOG.debug("REST request to get member org ids for member {}", salesforceId);
        try {
            MemberOrgIds memberOrgIds = memberService.getCurrentMemberOrgIds(salesforceId);
            return ResponseEntity.ok(memberOrgIds);
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * {@code GET  /members} : get all members.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the
     *         list of member in body.
     */
    @GetMapping("/members")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<Page<Member>> getAllMembers(@RequestParam(required = false, name = "filter") String filter,
            Pageable pageable) {
        LOG.debug("REST request to get a page of Member");
        Page<Member> page = null;
        if (StringUtils.isBlank(filter)) {
            page = memberService.getMembers(pageable);
        } else {
            String decodedFilter;
            try {
                decodedFilter = URLDecoder.decode(filter, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                /* try without decoding if this ever happens */
                decodedFilter = filter;
            }
            page = memberService.getMembers(pageable, decodedFilter);
        }
        return ResponseEntity.ok(page);
    }

    /**
     * {@code GET  /member} : get all the member.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the
     *         list of member in body.
     */
    @GetMapping("/members/list/all")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<List<Member>> getMembersList() {
        LOG.debug("REST request to get a page of Member");
        List<Member> members = memberService.getAllMembers();
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    /**
     * {@code GET  /members/:id} : get the "id" member.
     *
     * @param id - the id or salesforce id of the member to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the member, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/members/{id}")
    public ResponseEntity<Member> getMember(@PathVariable String id) {
        LOG.debug("REST request to get Member : {}", id);
        Optional<Member> member = memberService.getMember(id);
        if (!member.isPresent()) {
            LOG.warn("Can't find member with id {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(member.get());
    }

    /**
     * {@code GET  /members/authorized/:encryptedEmail} : get the authorized
     * member details for the specified encrypted email.
     *
     * @param encryptedEmail - the encrypted email of the user that has authorized
     *                       the
     *                       member
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         the member details in the body, or status
     *         {@code 404 (Not Found)}.
     */
    @GetMapping("/members/authorized/{encryptedEmail}")
    public ResponseEntity<Member> getAuthorizedMember(@PathVariable String encryptedEmail) {
        LOG.debug("REST request to get authorized Member details for encrypted email : {}", encryptedEmail);
        Optional<Member> member = memberService.getAuthorizedMemberForUser(encryptedEmail);
        if (!member.isPresent()) {
            LOG.warn("Can't find member for encrypted email");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(member.get());
    }

    @PostMapping("/members/{salesforceId}/contact-update")
    public ResponseEntity<MemberContactUpdateResponse> processMemberContactUpdate(
            @RequestBody MemberContactUpdate memberContactUpdate,
            @PathVariable String salesforceId) {
        LOG.debug("REST request to create new member contact update for member {}", salesforceId);
        try {
            memberService.processMemberContact(memberContactUpdate, salesforceId);
            return ResponseEntity.ok(new MemberContactUpdateResponse(true));
        } catch (UnauthorizedMemberAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PreAuthorize("hasRole(\"ROLE_CONSORTIUM_LEAD\")")
    @PostMapping("/members/add-consortium-member")
    public ResponseEntity<Void> requestNewConsortiumMember(@RequestBody AddConsortiumMember addConsortiumMember) {
        LOG.debug("REST request to request add new consortium member");
        memberService.requestNewConsortiumMember(addConsortiumMember);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole(\"ROLE_CONSORTIUM_LEAD\")")
    @PostMapping("/members/remove-consortium-member")
    public ResponseEntity<Void> requestRemoveConsortiumMember(
            @RequestBody RemoveConsortiumMember removeConsortiumMember) {
        LOG.debug("REST request to request remove consortium member");
        memberService.requestRemoveConsortiumMember(removeConsortiumMember);
        return ResponseEntity.ok().build();
    }

    private boolean memberDetailsUpdateValid(MemberUpdateData data) {
        if (StringUtils.isBlank(data.getPublicName())) {
            LOG.info("Null name in request to update public details");
            return false;
        }

        // allow null billing address but if present, country must be specified
        if (data.getBillingAddress() != null && StringUtils.isBlank(data.getBillingAddress().getCountry())) {
            LOG.info("Null billing country in request to update public details");
            return false;
        }
        return true;
    }
}
