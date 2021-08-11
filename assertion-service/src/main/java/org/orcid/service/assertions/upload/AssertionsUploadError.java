package org.orcid.service.assertions.upload;

import java.io.Serializable;

public class AssertionsUploadError implements Serializable {

    private static final long serialVersionUID = 1L;

    private long index;

    private String message;

    public AssertionsUploadError(long index, String message) {
        this.index = index;
        this.message = message;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
