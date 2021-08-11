package org.orcid.user.upload;

import java.io.IOException;
import java.io.InputStream;

import org.orcid.user.domain.User;

public interface UserUploadReader {

    UserUpload readUsersUpload(InputStream inputStream, User currentUser) throws IOException;

}
