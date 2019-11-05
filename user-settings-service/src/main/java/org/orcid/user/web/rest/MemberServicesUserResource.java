package org.orcid.user.web.rest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.service.dto.MemberServicesUserDTO;
import org.orcid.user.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing {@link org.orcid.user.domain.MemberServicesUser}.
 */
@RestController
@RequestMapping("/api")
public class MemberServicesUserResource {

    private final Logger log = LoggerFactory.getLogger(MemberServicesUserResource.class);

    private static final String ENTITY_NAME = "userSettingsServiceMemberServicesUser";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private Oauth2ServiceClient oauth2ServiceClient;
    
    private final UserSettingsRepository userSettingsRepository;

    public MemberServicesUserResource(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    /**
     * {@code POST  /member-services-users} : Create a new memberServicesUser.
     *
     * @param memberServicesUser the memberServicesUser to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new memberServicesUser, or with status {@code 400 (Bad Request)} if the memberServicesUser has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     * @throws JSONException 
     */
    @PostMapping("/member-services-users")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\")")
    public ResponseEntity<MemberServicesUserDTO> createMemberServicesUser(@Valid @RequestBody MemberServicesUserDTO memberServicesUserDTO) throws URISyntaxException, JSONException {
        log.debug("REST request to save MemberServicesUserDTO : {}", memberServicesUserDTO);
        if (memberServicesUserDTO.getId() != null) {
            throw new BadRequestAlertException("A new memberServicesUser cannot already have an ID", ENTITY_NAME, "idexists");
        }
        
        String login = memberServicesUserDTO.getLogin();
    	//String authorities = String.join(",", memberServicesUserDTO.getAuthorities());
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("login", login);
    	map.put("password", memberServicesUserDTO.getPassword());
    	map.put("email", memberServicesUserDTO.getEmail());
    	map.put("authorities", memberServicesUserDTO.getAuthorities());
    	
    	ResponseEntity<Void> response = oauth2ServiceClient.registerUser(map);
        if(response == null || !HttpStatus.CREATED.equals(response.getStatusCode())) {
        	throw new RuntimeException("User creation failed: " + response.getStatusCode().getReasonPhrase());
        }
    	
    	// Now fetch the user to get the user id and populate the member services user information
        ResponseEntity<String> userInfo = oauth2ServiceClient.getUser(login);
        
        String user = userInfo.getBody();
        JSONObject obj = new JSONObject(user);
        String userId = obj.getString("id");
        
        memberServicesUserDTO.setUserId(userId);
        
        // Remove the password, so we dont store plain passwords anywhere
        memberServicesUserDTO.setPassword(null);
        
        MemberServicesUserDTO result = memberServicesUserRepository.save(memberServicesUserDTO);
        return ResponseEntity.created(new URI("/api/member-services-users/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /member-services-users} : Updates an existing memberServicesUser.
     *
     * @param memberServicesUser the memberServicesUser to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated memberServicesUser,
     * or with status {@code 400 (Bad Request)} if the memberServicesUser is not valid,
     * or with status {@code 500 (Internal Server Error)} if the memberServicesUser couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/member-services-users")
    public ResponseEntity<MemberServicesUserDTO> updateMemberServicesUser(@Valid @RequestBody MemberServicesUserDTO memberServicesUserDTO) throws URISyntaxException {
        log.debug("REST request to update MemberServicesUserDTO : {}", memberServicesUserDTO);
        if (memberServicesUserDTO.getId() == null || memberServicesUserDTO.getUserId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MemberServicesUserDTO result = memberServicesUserRepository.save(memberServicesUserDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, memberServicesUserDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /member-services-users} : get all the memberServicesUsers.
     *
     * @param pageable the pagination information.
     * @param queryParams a {@link MultiValueMap} query parameters.
     * @param uriBuilder a {@link UriComponentsBuilder} URI builder.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of memberServicesUsers in body.
     */
    @GetMapping("/member-services-users")
    public ResponseEntity<List<MemberServicesUserDTO>> getAllMemberServicesUsers(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder) {
        log.debug("REST request to get a page of MemberServicesUsers");
        Page<MemberServicesUser> page = memberServicesUserRepository.findAll(pageable);
        //TODO: fetch User info from UAA and populate missing values in the MemberServicesUserDTO
        List<MemberServicesUserDTO> dtoList = new ArrayList<MemberServicesUserDTO>();
        
        for(MemberServicesUser msu : page) {
        	dtoList.add(MemberServicesUserDTO.valueOf(msu));
        }
        
        Page<MemberServicesUserDTO> dtoPage = new PageImpl<MemberServicesUserDTO>(dtoList, page.getPageable(), page.getTotalElements());
        
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), dtoPage);
        return ResponseEntity.ok().headers(headers).body(dtoPage.getContent());
    }

    /**
     * {@code GET  /member-services-users/:id} : get the "id" memberServicesUser.
     *
     * @param id the id of the memberServicesUser to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the memberServicesUser, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/member-services-users/{id}")
    public ResponseEntity<MemberServicesUserDTO> getMemberServicesUser(@PathVariable String id) {
        log.debug("REST request to get MemberServicesUserDTO : {}", id);
        Optional<MemberServicesUser> msu = memberServicesUserRepository.findById(id);
        if(!msu.isPresent()) {
        	return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().body(MemberServicesUserDTO.valueOf(msu.get()));
    }

    /**
     * {@code DELETE  /member-services-users/:id} : delete the "id" memberServicesUser.
     *
     * @param id the id of the memberServicesUser to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/member-services-users/{id}")
    public ResponseEntity<Void> deleteMemberServicesUser(@PathVariable String id) {
        log.debug("REST request to delete MemberServicesUserDTO : {}", id);
        memberServicesUserRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }
}
