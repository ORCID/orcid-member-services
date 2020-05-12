package org.orcid.service.assertions.upload;

import java.io.IOException;
import java.io.InputStream;

public interface AssertionsUploadReader {
	
	public AssertionsUpload readAssertionsUpload(InputStream inputStream) throws IOException;

}
