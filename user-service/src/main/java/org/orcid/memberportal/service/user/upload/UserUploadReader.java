package org.orcid.memberportal.service.user.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.memberportal.service.user.domain.User;

public interface UserUploadReader {

    UserUpload readUsersUpload(InputStream inputStream, User currentUser) throws IOException;

}
