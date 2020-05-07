package org.orcid.user.upload;

import java.io.IOException;
import java.io.InputStream;

public interface MembersUploadReader {
	
	public MembersUpload readMembersUpload(InputStream inputStream) throws IOException;

}
