package org.orcid.member.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.member.service.user.MemberServiceUser;

public interface MembersUploadReader {
	
	public MemberUpload readMemberUpload(InputStream inputStream, MemberServiceUser user) throws IOException;

}
