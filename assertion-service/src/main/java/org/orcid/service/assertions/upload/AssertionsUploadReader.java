package org.orcid.service.assertions.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.domain.AssertionServiceUser;

public interface AssertionsUploadReader {

    public AssertionsUpload readAssertionsUpload(InputStream inputStream, AssertionServiceUser user) throws IOException;

}
