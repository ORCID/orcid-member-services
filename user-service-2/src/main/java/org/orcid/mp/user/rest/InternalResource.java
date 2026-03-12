package org.orcid.mp.user.rest;

import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/internal/users")
public class InternalResource {

    private final Logger LOG = LoggerFactory.getLogger(InternalResource.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    /**
     * Endpoint for internal clients to access user.
     *
     * {@code GET  /internal/users/:loginOrId}
     *
     * @param loginOrId - the id or login of the user to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with
     *         body the user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{loginOrId}")
    public ResponseEntity<UserDTO> getUserByLogin(@PathVariable String loginOrId) {
        LOG.debug("REST request to get User : {}", loginOrId);
        Optional<User> user = userService.getUserByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUser(loginOrId);
        }
        if (!user.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userMapper.toUserDTO(user.get()));
    }

}
