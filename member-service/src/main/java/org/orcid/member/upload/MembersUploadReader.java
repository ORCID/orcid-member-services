package org.orcid.member.upload;

import java.io.IOException;
import java.io.InputStream;

public interface MembersUploadReader {
	
	public MemberUpload readMemberUpload(InputStream inputStream) throws IOException;

}
