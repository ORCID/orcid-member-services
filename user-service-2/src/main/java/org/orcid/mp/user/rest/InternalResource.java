package org.orcid.mp.user.rest;

import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<UserDTO> getUserByLoginOrId(@PathVariable String loginOrId) {
        LOG.debug("Internal request to get User : {}", loginOrId);
        Optional<User> user = userService.getUserByLogin(loginOrId);
        if (!user.isPresent()) {
            user = userService.getUser(loginOrId);
        }
        if (!user.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userMapper.toUserDTO(user.get()));
    }

    /**
     * {@code PUT /internal/users/memberName/:oldMemberName/:newMemberName} : Updates memberName
     * for existing Users.
     *
     * @param memberId  the memberId for finding users to update
     * @param newMemberName the new Value of the memberName to update
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/memberName/{memberId}/{newMemberName}")
    public ResponseEntity<Void> updateUsersMemberName(@PathVariable String memberId, @PathVariable String newMemberName) {
        LOG.info("Internal request to update users' member names id to {}", newMemberName);
        boolean success = userService.updateUsersMemberName(memberId, newMemberName);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * {@code POST /internal/users} : Creates main contact user
     */
    @PostMapping("/users")
    public ResponseEntity<String> createMainContactUser(@RequestBody User user) {
        LOG.info("Internal request to create main contact user for member {}", user.getMemberId());
        try {
            UserDTO userDTO = userMapper.toUserDTO(user);
            userService.createUser(userDTO, userDTO.getCreatedBy());
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            LOG.error("Error creating main contact user", e);
            return ResponseEntity.status(500).build();
        }
    }

}
