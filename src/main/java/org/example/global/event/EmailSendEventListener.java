package org.example.global.event;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 이메일 발송을 실제로 수행하는 컴포넌트. 같은 클래스 내부 호출로 인해 @Async가 무효화되는 문제를 피하기 위해
// EmailVerificationService/PasswordResetService와 완전히 분리된 별도 빈으로 둔다.
// @TransactionalEventListener(AFTER_COMMIT)로 등록되어 있어, 인증번호/토큰 저장 트랜잭션이 롤백되면
// 이 리스너 자체가 호출되지 않는다 — DB 저장 실패 후 이메일만 발송되는 상황을 방지한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendEventListener {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmailSendRequestedEvent event) {
        try {
            if (event.isHtml()) {
                sendHtml(event);
            } else {
                sendPlainText(event);
            }
        } catch (MailException | jakarta.mail.MessagingException e) {
            // 인증번호/토큰 등 민감정보는 이벤트 본문에 포함되어 있으므로 절대 로그에 남기지 않는다.
            // 실패 원인과 수신자 도메인만 남겨 운영 중 원인 파악이 가능하도록 한다.
            log.error("Failed to send email. domain={}, reason={}", maskedDomain(event.getTo()), e.getMessage());
        }
    }

    private void sendPlainText(EmailSendRequestedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(event.getTo());
        message.setSubject(event.getSubject());
        message.setText(event.getBody());
        mailSender.send(message);
    }

    private void sendHtml(EmailSendRequestedEvent event) throws jakarta.mail.MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setTo(event.getTo());
        helper.setSubject(event.getSubject());
        helper.setText(event.getBody(), true);
        mailSender.send(mimeMessage);
    }

    // 이메일 전체를 로그에 남기지 않고 도메인만 남겨 어느 메일 서비스에서 실패했는지 정도만 확인 가능하게 함
    private String maskedDomain(String email) {
        int at = email.indexOf('@');
        return at >= 0 ? "***" + email.substring(at) : "***";
    }
}
