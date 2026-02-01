package com.m2nsteel.bank_program_modernization.dto;

import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NullMarked;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NullMarked
@Schema(description = "거래 관련 데이터 전송 객체 (DTO)")
public class TransactionDto {

    @Schema(description = "입금 요청")
    public record DepositRequest(
            @NotBlank
            @Schema(description = "계좌 번호", example = "123-456-789012", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountNumber,

            @NotNull @Positive
            @Schema(description = "입금 금액", example = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
            Long amount,

            @NotBlank
            @Schema(description = "멱등성 키 (중복 요청 방지용 UUID)", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
            String idempotencyKey
    ) {}

    @Schema(description = "출금 요청")
    public record WithdrawRequest(
            @NotBlank
            @Schema(description = "계좌 번호", example = "123-456-789012", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountNumber,

            @NotNull @Positive
            @Schema(description = "출금 금액", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
            Long amount,

            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "계좌 비밀번호", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountPassword,

            @NotBlank
            @Schema(description = "멱등성 키", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
            String idempotencyKey
    ) {}

    @Schema(description = "이체 요청")
    public record TransferRequest(
            @NotBlank
            @Schema(description = "출금 계좌 번호", example = "123-456-789012", requiredMode = Schema.RequiredMode.REQUIRED)
            String fromAccountNumber,

            @NotBlank
            @Schema(description = "입금 계좌 번호", example = "987-654-321098", requiredMode = Schema.RequiredMode.REQUIRED)
            String toAccountNumber,

            @NotNull @Positive
            @Schema(description = "이체 금액", example = "30000", requiredMode = Schema.RequiredMode.REQUIRED)
            Long amount,

            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "출금 계좌 비밀번호", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountPassword,

            @NotBlank
            @Schema(description = "멱등성 키", example = "b1a2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8", requiredMode = Schema.RequiredMode.REQUIRED)
            String idempotencyKey
    ) {}

    @Schema(description = "일반 거래 응답 (입/출금)")
    public record GeneralResponse(
            @Schema(description = "거래 고유 식별자 (UUID)", example = "tx-7788-9900")
            String txExternalId,

            @Schema(description = "계좌 번호", example = "123-456-789012")
            String accountNumber,

            @Schema(description = "거래 타입", example = "DEPOSIT", allowableValues = {"DEPOSIT", "WITHDRAWAL"})
            String type,

            @Schema(description = "거래 금액", example = "10000")
            Long amount,

            @Schema(description = "거래 후 잔액", example = "110000")
            Long balanceAfter,

            @Schema(description = "거래 상태", example = "SUCCESS")
            String status,

            @Schema(description = "거래 일시")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "이체 거래 응답")
    public record TransferResponse(
            @Schema(description = "거래 고유 식별자 (UUID)", example = "tx-transfer-1122")
            String txExternalId,

            @Schema(description = "출금 계좌 번호", example = "123-456-789012")
            String fromAccountNumber,

            @Schema(description = "입금 계좌 번호", example = "987-654-321098")
            String toAccountNumber,

            @Schema(description = "이체 금액", example = "30000")
            Long amount,

            @Schema(description = "출금 계좌 거래 후 잔액", example = "70000")
            Long balanceAfter,

            @Schema(description = "거래 상태", example = "SUCCESS")
            String status,

            @Schema(description = "거래 일시")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "거래 내역 조회 조건")
    public record TransactionSearchRequest(
            @NotNull
            @Schema(description = "계좌 식별자 (UUID)", example = "acc-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountExternalId,

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Schema(description = "조회 시작일", example = "2026-01-01")
            LocalDate startDate,

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Schema(description = "조회 종료일", example = "2026-02-01")
            LocalDate endDate,

            @Schema(description = "거래 유형 (DEPOSIT, WITHDRAWAL, TRANSFER)", example = "TRANSFER")
            TransactionType type,

            @Schema(description = "최소 금액", example = "1000")
            Long minAmount,

            @Schema(description = "최대 금액", example = "100000")
            Long maxAmount,

            @Schema(description = "마지막으로 조회된 ID (커서용)", example = "100")
            Long lastId
    ) {}

    @Schema(description = "거래 내역 응답")
    public record TransactionHistoryResponse(
            @Schema(description = "거래 식별자", example = "tx-abcd-efgh")
            String txExternalId,

            @Schema(description = "거래 유형", example = "WITHDRAWAL")
            String type,

            @Schema(description = "거래 총액 (절대값)", example = "50000")
            Long amount,

            @Schema(description = "실제 변동 금액 (입금 +, 출금 -)", example = "-50000")
            Long delta,

            @Schema(description = "거래 후 잔액", example = "150000")
            Long balanceAfter,

            @Schema(description = "거래 일시")
            LocalDateTime createdAt
    ) {}
}