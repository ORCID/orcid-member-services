package org.orcid.mp.member.rest;

import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MemberResource {

    private static final Logger LOG = LoggerFactory.getLogger(MemberResource.class);

    @GetMapping("/secure")
    public ResponseEntity<String> secure() {
        return ResponseEntity.ok("failure");
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<Member> test(@PathVariable String memberId) {
        LOG.info("Request for member {} from user {}", memberId, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok(new Member());
    }

}
