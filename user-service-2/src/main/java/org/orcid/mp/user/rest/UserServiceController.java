package org.orcid.mp.user.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @CrossOrigin??
public class UserServiceController {

    @GetMapping("/unprotected")
    public ResponseEntity<String> unprotected() {
        return ResponseEntity.ok("unprotected");
    }

    @GetMapping("/secure")
    public ResponseEntity<String> secure() {
        return ResponseEntity.ok("secure");
    }

}
