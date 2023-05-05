package org.orcid.memberportal.service.member.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.memberportal.service.member.services.pojo.MemberServiceUser;

public interface MembersUploadReader {

    public MemberUpload readMemberUpload(InputStream inputStream, MemberServiceUser user) throws IOException;

}
