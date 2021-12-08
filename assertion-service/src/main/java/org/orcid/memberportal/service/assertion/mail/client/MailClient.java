package org.orcid.memberportal.service.assertion.mail.client;

import java.io.File;

import org.orcid.memberportal.service.assertion.mail.MailException;

public interface MailClient {

    void sendMailWithAttachment(String to, String subject, String html, File attachment) throws MailException;

    void sendMail(String to, String subject, String html) throws MailException;

}
