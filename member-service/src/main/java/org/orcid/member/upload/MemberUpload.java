package org.orcid.member.upload;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.member.domain.Member;

public class MemberUpload {

    private JSONArray errors = new JSONArray();

    private List<Member> members = new ArrayList<>();

    public void addMember(Member member) {
        members.add(member);
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

    public JSONArray getErrors() {
        return errors;
    }

    public void setErrors(JSONArray errors) {
        this.errors = errors;
    }

    public List<Member> getMembers() {
        return members;
    }

}
