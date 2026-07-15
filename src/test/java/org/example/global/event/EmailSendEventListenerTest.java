package org.example.global.event;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSendEventListenerTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailSendEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new EmailSendEventListener(mailSender);
        ReflectionTestUtils.setField(listener, "fromAddress", "no-reply@novelnestia.com");
    }

    @Test
    void 평문_이메일은_SimpleMailMessage로_발송한다() {
        EmailSendRequestedEvent event = new EmailSendRequestedEvent("user@example.com", "제목", "본문", false);

        listener.handle(event);

        verify(mailSender).send((SimpleMailMessage) org.mockito.ArgumentMatchers.any());
    }

    @Test
    void HTML_이메일은_MimeMessage로_발송한다() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        EmailSendRequestedEvent event = new EmailSendRequestedEvent("user@example.com", "제목", "<b>본문</b>", true);

        listener.handle(event);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void 발송_실패해도_예외를_밖으로_전파하지_않는다() {
        EmailSendRequestedEvent event = new EmailSendRequestedEvent("user@example.com", "제목", "본문", false);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP 오류"))
                .when(mailSender).send((SimpleMailMessage) org.mockito.ArgumentMatchers.any());

        // 비동기 메서드 내부에서 예외를 삼키므로 호출자에게는 아무 예외도 전달되지 않아야 한다
        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
    }
}
