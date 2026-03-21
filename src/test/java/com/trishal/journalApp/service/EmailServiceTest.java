package com.trishal.journalApp.service;

import com.trishal.journalApp.exception.JournalAppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmail_shouldBuildAndSendMessage() {
        emailService.sendEmail("user@example.com", "Subject", "Body text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Subject");
        assertThat(sent.getText()).isEqualTo("Body text");
    }

    @Test
    void sendEmail_shouldThrowJournalAppException_whenSendFails() {
        doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "Body"))
                .isInstanceOf(JournalAppException.class);
    }
}
