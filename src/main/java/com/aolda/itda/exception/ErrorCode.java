package com.aolda.itda.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    INVALID_USER_INFO(HttpStatus.BAD_REQUEST, "잘못된 회원 정보입니다"),

    // Token
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 토큰입니다"),

    //Forwarding
    FAIL_CREATE_CONF(HttpStatus.BAD_REQUEST, "Conf 파일을 생성하지 못했습니다"),
    FAIL_UPDATE_CONF(HttpStatus.BAD_REQUEST, "Conf 파일을 수정하지 못했습니다"),
    NOT_FOUND_FORWARDING(HttpStatus.BAD_REQUEST, "포트포워딩 파일이 존재하지 않습니다"),
    INVALID_CONF_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력이 존재합니다"),
    DUPLICATED_INSTANCE_INFO(HttpStatus.BAD_REQUEST, "중복된 인스턴스 IP와 포트입니다"),
    DUPLICATED_SERVER_PORT(HttpStatus.BAD_REQUEST, "중복된 서버 포트입니다");

    private final HttpStatus status;
    private final String message;
}
