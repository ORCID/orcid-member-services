package org.orcid.mp.user.upload;

import org.orcid.mp.user.dto.UserDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserUpload {

    List<UserDTO> userDTOs = new ArrayList<>();

    List<Map<String, Object>> errors = new ArrayList<>();

    public void addUserDTO(UserDTO userDTO) {
        userDTOs.add(userDTO);
    }

    public void addError(long index, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("index", index);
        error.put("message", message);
        errors.add(error);
    }

    public List<UserDTO> getUserDTOs() {
        return userDTOs;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

}
