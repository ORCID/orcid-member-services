package org.orcid.memberportal.service.member.mail.client;

import org.orcid.memberportal.service.member.mail.MailException;

import java.io.File;

public interface MailClient {

    void sendMailWithAttachment(String to, String subject, String html, File attachment) throws MailException;

    void sendMail(String to, String subject, String html) throws MailException;

}
