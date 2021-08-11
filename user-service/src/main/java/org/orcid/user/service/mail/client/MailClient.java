package org.orcid.user.service.mail.client;

import org.orcid.user.service.mail.MailException;

public interface MailClient {

    void sendMail(String to, String subject, String html) throws MailException;

}
