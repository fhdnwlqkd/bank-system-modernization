package com.m2nsteel.bank_program_modernization.core.exception;

import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리 (아이디 중복, 비밀번호 불일치 등)
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: code={}, message={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());

        ErrorCode errorCode = e.getErrorCode();

        // 1. ErrorResponse 생성
        ExceptionResponse errorResponse = new ExceptionResponse(
                errorCode.getCode(),
                errorCode.name(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                List.of() // 비즈니스 예외는 보통 필드 에러가 없으니 빈 리스트 전달
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorResponse));
    }

    /**
     * 예측하지 못한 서버 내부 오류 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);

        // 1. 공통 INTERNAL_SERVER_ERROR를 위한 ErrorResponse 생성
        ExceptionResponse errorResponse = new ExceptionResponse(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.",
                LocalDateTime.now(),
                List.of()
        );

        // 2. ApiResponse 규격에 맞춰 리턴
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(errorResponse));
    }
}