package org.orcid.user.upload;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.service.dto.UserDTO;

public class UsersUpload {
	
	List<UserDTO> userDTOs = new ArrayList<>();
	
	List<UserSettings> userSettings = new ArrayList<>();
	
	JSONArray errors = new JSONArray();
	
	public void addUserDTO(UserDTO userDTO) {
		userDTOs.add(userDTO);
	}
	
	public void addUserSettgins(UserSettings user) {
		userSettings.add(user);
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

	public List<UserSettings> getUserSettings() {
		return userSettings;
	}

	public JSONArray getErrors() {
		return errors;
	}

}
