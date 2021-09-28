package org.orcid.memberportal.service.user.upload;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.memberportal.service.user.dto.UserDTO;

public class UserUpload {

    List<UserDTO> userDTOs = new ArrayList<>();

    JSONArray errors = new JSONArray();

    public void addUserDTO(UserDTO userDTO) {
        userDTOs.add(userDTO);
    }

    public void addError(long index, String message) {
        JSONObject error = new JSONObject();
        try {
            error.put("index", index);
            error.put("message", message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        errors.put(error);
    }

    public List<UserDTO> getUserDTOs() {
        return userDTOs;
    }

    public JSONArray getErrors() {
        return errors;
    }

}
