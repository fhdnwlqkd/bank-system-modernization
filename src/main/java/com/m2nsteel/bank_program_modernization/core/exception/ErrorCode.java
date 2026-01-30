package com.m2nsteel.bank_program_modernization.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common Errors
    INVALID_INPUT("C001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("C002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_MEMBER_TYPE("C003", "유효하지 않은 회원 유형입니다.", HttpStatus.BAD_REQUEST),

    // member
    DUPLICATE_LOGIN_ID("M001", "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),
    MEMBER_NOT_FOUND("M002", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD("M003", "비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    MEMBER_NOT_ACTIVE("M004", "비활성화된 회원입니다.", HttpStatus.FORBIDDEN),
    MERCHANT_NOT_FOUND("M005", "가맹점을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // account
    INSUFFICIENT_BALANCE("A001", "잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND("A002", "존재하지 않는 계좌입니다.", HttpStatus.NOT_FOUND),
    INVALID_ACCOUNT_PASSWORD("A003", "계좌 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    CONCURRENCY_CONFLICT("A004", "동시 요청으로 처리에 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.CONFLICT),
    ACCOUNT_CLOSED("A005", "폐쇄된 계좌입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCOUNT_ACCESS("A006", "계좌에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_ACCOUNT_OWNER("A007", "계좌 소유자가 아닙니다.", HttpStatus.FORBIDDEN),

    // transaction
    DUPLICATE_REQUEST("T001", "이미 처리된 요청입니다.", HttpStatus.CONFLICT),

    // Card
    CARD_NOT_FOUND("CRD001", "존재하지 않는 카드입니다.", HttpStatus.NOT_FOUND),
    CARD_NOT_ACTIVE("CRD002", "비활성화된 카드입니다.", HttpStatus.BAD_REQUEST),
    SELF_PAYMENT_NOT_ALLOWED("T002", "본인 계좌로의 이체는 허용되지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_CARD_PASSWORD("CRD003", "카드 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    NOT_CARD_OWNER("CRD004", "카드 소유자가 아닙니다.", HttpStatus.FORBIDDEN),

    // Refund
    EXCEED_REFUND_AMOUNT("R001", "환불 금액이 결제 금액을 초과합니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND("P001", "존재하지 않는 결제입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
