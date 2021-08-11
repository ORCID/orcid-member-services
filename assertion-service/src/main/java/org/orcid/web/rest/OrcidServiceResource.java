package org.orcid.web.rest;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.security.SecurityUtils;
import org.orcid.service.OrcidRecordService;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class OrcidServiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrcidRecordService.class);

    @Autowired
    private OrcidRecordService orcidRecordService;

    @GetMapping("/orcid_records")
    public ResponseEntity<List<OrcidRecord>> getOrcidRecords(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws BadRequestAlertException, JSONException {
        Page<OrcidRecord> records = orcidRecordService.findBySalesforceId(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), records);
        return ResponseEntity.ok().headers(headers).body(records.getContent());
    }

    @GetMapping("/orcid_record/{id}")
    public ResponseEntity<OrcidRecord> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch orcid record {} from user {}", id, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok().body(orcidRecordService.findById(id));
    }

}
