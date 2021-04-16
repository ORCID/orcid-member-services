package org.orcid.service.assertions.upload;

import java.io.Serializable;

import org.codehaus.jettison.json.JSONArray;

public class AssertionsUploadSummary implements Serializable {

	private static final long serialVersionUID = 1L;

	private int numAdded;
	
	private int numUpdated;
	
	private int numDeleted;
	
	private int numDuplicates;
	
	private JSONArray errors = new JSONArray();

	public int getNumAdded() {
		return numAdded;
	}

	public void setNumAdded(int numAdded) {
		this.numAdded = numAdded;
	}

	public int getNumUpdated() {
		return numUpdated;
	}

	public void setNumUpdated(int numUpdated) {
		this.numUpdated = numUpdated;
	}

	public int getNumDeleted() {
		return numDeleted;
	}

	public void setNumDeleted(int numDeleted) {
		this.numDeleted = numDeleted;
	}

	public int getNumDuplicates() {
		return numDuplicates;
	}

	public void setNumDuplicates(int numDuplicates) {
		this.numDuplicates = numDuplicates;
	}

	public JSONArray getErrors() {
		return errors;
	}

	public void setErrors(JSONArray errors) {
		this.errors = errors;
	}
	
}
