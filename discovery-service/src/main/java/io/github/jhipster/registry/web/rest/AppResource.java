package io.github.jhipster.registry.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.jhipster.registry.config.ApplicationProperties;
import io.github.jhipster.registry.service.dto.VersionDTO;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AppResource {

    private final Logger log = LoggerFactory.getLogger(AppResource.class);
    
    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/version")
    public ResponseEntity<VersionDTO> getAppVersion() {
        log.debug("REST request to get application version");
        String version = applicationProperties.getAppVersion();
        return ResponseEntity.ok(new VersionDTO(version));
    }

}
