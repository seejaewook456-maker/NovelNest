package org.example.domain.emailverification.entity;

// 이메일 인증번호의 용도 구분 — 같은 이메일이라도 목적이 다르면 서로 다른 인증 건으로 취급한다.
public enum Purpose {
    SIGN_UP,
    PASSWORD_RESET
}
