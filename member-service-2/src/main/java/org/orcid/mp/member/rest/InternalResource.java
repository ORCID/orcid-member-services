package org.orcid.mp.member.rest;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import org.codehaus.jettison.json.JSONException;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.MemberContactUpdateResponse;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.upload.MemberUpload;
import org.orcid.mp.member.validation.MemberValidation;
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/internal/members")
public class InternalResource {

    private final Logger LOG = LoggerFactory.getLogger(InternalResource.class);

    @Autowired
    private MemberService memberService;

    /**
     * Endpoint for internal clients to access member.
     *
     * {@code GET  /internal/members/:id} : get the "id" member.
     *
     * @param id - the id or salesforce id of the member to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the member, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMember(@PathVariable String id) {
        LOG.debug("REST request to get Member : {}", id);
        Optional<Member> member = memberService.getMember(id);
        if (!member.isPresent()) {
            LOG.warn("Can't find member with id {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(member.get());
    }

}
