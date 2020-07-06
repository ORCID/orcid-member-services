package org.orcid.user.web.rest.errors;

import java.util.HashMap;
import java.util.Map;

public class MemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

    private static final String PARAM = "param";

    private String message = "";

    private final Map<String, String> paramMap = new HashMap<>();

    public MemberNotFoundException(String message) {
		super(message);
	}

    public MemberNotFoundException(String message, String... params) {
        super(message);

        this.message = message;
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                paramMap.put(PARAM + i, params[i]);
            }
        }
    }

    public MemberNotFoundException(String message, Map<String, String> paramMap) {
        super(message);
        this.message = message;
        this.paramMap.putAll(paramMap);
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }
}
