package org.orcid.memberportal.service.member.client.model;

import java.util.List;

public class Country {

    private String code;

    private String name;

    private List<State> states;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }
}

