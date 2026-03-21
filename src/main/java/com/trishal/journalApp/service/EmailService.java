package com.trishal.journalApp.service;

import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * Sends a plain-text email. Throws {@link JournalAppException} (ERR_4003)
     * if delivery fails so callers can handle it explicitly.
     */
    public void sendEmail(String toMail, String subject, String body) {
        try {
            SimpleMailMessage mail = buildMessage(toMail, subject, body);
            javaMailSender.send(mail);
            log.info("Email sent successfully to {}", toMail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toMail, e.getMessage(), e);
            throw new JournalAppException(ErrorCode.EMAIL_SEND_FAILED, e);
        }
    }

    private SimpleMailMessage buildMessage(String to, String subject, String body) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(body);
        return mail;
    }
}