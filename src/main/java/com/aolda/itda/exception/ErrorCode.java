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

    // Forwarding
    NOT_FOUND_FORWARDING(HttpStatus.BAD_REQUEST, "포트포워딩 파일이 존재하지 않습니다"),

    // Routing
    NOT_FOUND_ROUTING(HttpStatus.BAD_REQUEST, "라우팅 파일이 존재하지 않습니다"),

    // Certificate
    NOT_FOUND_CERTIFICATE(HttpStatus.BAD_REQUEST, "SSL 인증서가 존재하지 않습니다"),

    // CONF File
    FAIL_CREATE_CONF(HttpStatus.BAD_REQUEST, "Conf 파일을 생성하지 못했습니다"),
    FAIL_UPDATE_CONF(HttpStatus.BAD_REQUEST, "Conf 파일을 수정하지 못했습니다"),
    INVALID_CONF_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력이 존재합니다"),
    DUPLICATED_INSTANCE_INFO(HttpStatus.BAD_REQUEST, "중복된 인스턴스 IP와 포트입니다"),
    DUPLICATED_SERVER_PORT(HttpStatus.BAD_REQUEST, "중복된 서버 포트입니다"),
    DUPLICATED_DOMAIN_NAME(HttpStatus.BAD_REQUEST, "중복된 도메인 주소입니다"),

    // Nginx
    FAIL_NGINX_CONF_TEST(HttpStatus.BAD_REQUEST, "Conf 파일 테스트에 실패했습니다"),
    FAIL_NGINX_CONF_RELOAD(HttpStatus.BAD_REQUEST, "Nginx 재시작에 실패했습니다"),

    FAIL_DELETE_CONF(HttpStatus.BAD_REQUEST, "Conf 파일을 삭제하지 못했습니다"),
    FAIL_ROLL_BACK(HttpStatus.BAD_REQUEST, "롤백 실패");

    private final HttpStatus status;
    private final String message;
}
