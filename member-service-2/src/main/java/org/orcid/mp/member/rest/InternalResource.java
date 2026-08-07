package org.orcid.mp.member.rest;

import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
