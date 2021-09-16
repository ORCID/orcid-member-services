package org.orcid.memberportal.service.assertion.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;

public interface AssertionsUploadReader {

    public AssertionsUpload readAssertionsUpload(InputStream inputStream, AssertionServiceUser user) throws IOException;

}
