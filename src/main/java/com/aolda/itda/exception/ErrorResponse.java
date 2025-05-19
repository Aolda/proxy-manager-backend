package com.aolda.itda.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private final HttpStatus status;  // HTTP 상태 코드
    private final String code;        // 에러 코드
    private final String message;     // 에러 메시지

    public static ResponseEntity<ErrorResponse> fromException(CustomException e) {
        String message = e.getErrorCode().getMessage();
        if (e.getInfo() != null) {
            message += " " + e.getInfo(); // 추가 정보가 있는 경우 결합
        }
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.builder()
                        .status(e.getErrorCode().getStatus())
                        .code(e.getErrorCode().name())
                        .message(message)
                        .build());
    }
}
