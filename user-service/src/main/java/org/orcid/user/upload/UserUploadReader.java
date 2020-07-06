package org.orcid.user.upload;

import java.io.IOException;
import java.io.InputStream;

public interface UserUploadReader {

	UserUpload readUsersUpload(InputStream inputStream, String createdBy) throws IOException;
	
}
