package com.m2nsteel.bank_program_modernization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NullMarked
@Schema(description = "카드 관련 데이터 전송 객체 (DTO)")
public class CardDto {

    @Schema(description = "카드 발급 요청")
    public record CardIssueRequest(
            @NotBlank
            @Schema(description = "연결할 계좌 번호", example = "123-456-789012", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountNumber,

            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "계좌 비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "계좌 비밀번호 (숫자 4자리)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountPassword,

            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "카드 비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "카드 비밀번호 (숫자 4자리)", example = "5678", requiredMode = Schema.RequiredMode.REQUIRED)
            String cardPassword,

            @NotBlank
            @Schema(description = "카드 종류 (현재 CHECK만 지원)", example = "CHECK", allowableValues = {"CHECK"}, requiredMode = Schema.RequiredMode.REQUIRED)
            String cardType
    ) {}

    @Schema(description = "카드 정보 응답")
    public record CardResponse(
            @Schema(description = "카드 고유 식별자 (UUID)", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
            String externalId,

            @Schema(description = "카드 번호", example = "9410-1234-5678-9012")
            String cardNumber,

            @Schema(description = "연결된 계좌 번호", example = "123-456-789012")
            String accountNumber,

            @Schema(description = "카드 종류", example = "CHECK")
            String cardType,

            @Schema(description = "카드 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "CLOSED"})
            String status,

            @Schema(description = "유효 기간", example = "2031-02-01")
            LocalDate expiredAt,

            @Schema(description = "카드 발급 일시")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "카드 결제 요청")
    public record CardPaymentRequest(
            @NotBlank
            @Schema(description = "결제할 카드 식별자 (UUID)", example = "card-abcd-1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String cardExternalId,

            @NotNull @Positive
            @Schema(description = "결제 요청 금액", example = "50000", requiredMode = Schema.RequiredMode.REQUIRED)
            Long amount,

            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "카드 비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "카드 비밀번호 (4자리)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String password,

            @NotBlank
            @Schema(description = "가맹점 사업자 등록 번호", example = "123-45-67890", requiredMode = Schema.RequiredMode.REQUIRED)
            String businessNumber,

            @NotBlank
            @Schema(description = "멱등성 키 (중복 결제 방지용)", example = "pay-key-0001", requiredMode = Schema.RequiredMode.REQUIRED)
            String idempotencyKey
    ) {}

    @Schema(description = "카드 결제 환불 요청")
    public record RefundRequest(
            @NotBlank
            @Schema(description = "원본 결제 건 식벨자 (UUID)", example = "pay-5566-7788", requiredMode = Schema.RequiredMode.REQUIRED)
            String paymentExternalId,

            @NotNull @Positive
            @Schema(description = "환불 요청 금액 (부분 환불 가능)", example = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
            Long amount,

            @NotBlank
            @Schema(description = "환불 사유", example = "고객 단순 변심")
            String reason,

            @NotBlank
            @Schema(description = "멱등성 키 (중복 환불 방지용)", example = "ref-key-9999", requiredMode = Schema.RequiredMode.REQUIRED)
            String idempotencyKey
    ) {}

    @Schema(description = "카드 결제 응답")
    public record CardPaymentResponse(
            @Schema(description = "결제 건 식별자 (UUID)", example = "pay-abcd-1234")
            String paymentExternalId,

            @Schema(description = "연결된 거래 식별자 (UUID)", example = "tx-9988-7766")
            String transactionExternalId,

            @Schema(description = "마스킹된 카드 번호", example = "9410-****-****-1234")
            String maskedCardNumber,

            @Schema(description = "최종 결제 금액", example = "50000")
            Long amount,

            @Schema(description = "출금 계좌 번호", example = "123-456-789012")
            String accountNumber,

            @Schema(description = "결제 후 계좌 잔액", example = "450000")
            Long balanceAfter,

            @Schema(description = "가맹점명", example = "한양치킨 본점")
            String merchantName,

            @Schema(description = "결제 상태", example = "APPROVED", allowableValues = {"APPROVED", "CANCELLED", "PARTIAL_REFUNDED"})
            String status,

            @Schema(description = "결제 일시")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "결제 환불 응답")
    public record RefundResponse(
            @Schema(description = "이번 환불 건의 식별자 (UUID)", example = "ref-1122-3344")
            String refundExternalId,

            @Schema(description = "이번 환불 거래의 식별자 (UUID)", example = "tx-ref-5566")
            String refundTxExternalId,

            @Schema(description = "원본 결제 식별자 (UUID)", example = "pay-abcd-1234")
            String originalPaymentExternalId,

            @Schema(description = "원본 전체 결제 금액", example = "50000")
            Long originalAmount,

            @Schema(description = "이번에 환불 처리된 금액", example = "10000")
            Long refundAmount,

            @Schema(description = "지금까지의 누적 환불 금액", example = "30000")
            Long totalRefundedAmount,

            @Schema(description = "취소 가능한 남은 잔액", example = "20000")
            Long remainingAmount,

            @Schema(description = "환불 사유", example = "고객 단순 변심")
            String reason,

            @Schema(description = "환불 후 원본 결제 상태", example = "PARTIAL_REFUNDED")
            String status,

            @Schema(description = "환불 처리 일시")
            LocalDateTime createdAt
    ) {}
}