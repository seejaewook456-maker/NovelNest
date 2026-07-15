package org.example.global.event;

import lombok.Getter;

// 트랜잭션 커밋 이후 비동기로 처리될 이메일 발송 요청. DB 저장이 롤백되면 이 이벤트 자체가 발행되지 않도록
// (또는 리스너가 커밋 후에만 반응하도록) 호출부에서 트랜잭션 내부에 publishEvent만 호출한다.
@Getter
public class EmailSendRequestedEvent {

    private final String to;
    private final String subject;
    private final String body;
    private final boolean html;

    public EmailSendRequestedEvent(String to, String subject, String body, boolean html) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.html = html;
    }
}
