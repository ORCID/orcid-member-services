package org.orcid.service.assertions.upload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.domain.Assertion;

public class AssertionsUpload {
	
	List<Assertion> assertions = new ArrayList<>();
	
	Set<String> users = new HashSet<>();
	
	JSONArray errors = new JSONArray();
	
	public void addAssertion(Assertion assertion) {
		assertions.add(assertion);
	}
	
	public void removeAssertion(Assertion assertion) {
		assertions.remove(assertion);
	}
	
	public void addUser(String user) {
		users.add(user);
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

	public List<Assertion> getAssertions() {
		return assertions;
	}

	public Set<String> getUsers() {
		return users;
	}
	
	public JSONArray getErrors() {
		return errors;
	}
	
	public static class AssertionsUploadDate {
		
		String year;
		String month;
		String day;
		
		public AssertionsUploadDate(String year, String month, String day) {
			this.year = year;
			this.month = month;
			this.day = day;
		}

		public String getYear() {
			return year;
		}

		public String getMonth() {
			return month;
		}

		public String getDay() {
			return day;
		}
		
	}
	
}
