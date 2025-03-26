package com.aolda.itda.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("[handleCustomException] {} : {}, {}", e.getErrorCode().name(), e.getErrorCode().getMessage(), e.getStackTrace());
        return ErrorResponse.fromException(e);
    }
}
