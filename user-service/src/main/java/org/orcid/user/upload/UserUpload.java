package org.orcid.user.upload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.service.dto.UserDTO;

public class UserUpload {
	
	List<UserDTO> userDTOs = new ArrayList<>();
	
	JSONArray errors = new JSONArray();
	private Map<String, String> orgWithOwner = new HashMap<String, String>();
	
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
	
	public void setOrgWithOwner(Map<String, String> orgWithOwner) {
		this.orgWithOwner = orgWithOwner;
	}
	
	public Map<String, String> getOrgWithOwner() {
		return this.orgWithOwner;
	}

}
