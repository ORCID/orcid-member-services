package org.orcid.memberportal.service.user.mail.client;

import org.orcid.memberportal.service.user.mail.MailException;

public interface MailClient {

    void sendMail(String to, String subject, String html) throws MailException;

}
