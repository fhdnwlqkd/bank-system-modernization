package com.m2nsteel.bank_program_modernization.dto;

import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NullMarked;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NullMarked
@Schema(description = "가맹점 관련 데이터 전송 객체 (DTO)")
public class MerchantDto {

    @Schema(description = "가맹점 결제 내역 검색 요청")
    public record MerchantPaymentSearchRequest(
            @Schema(description = "결제 상태 필터", example = "SUCCESS")
            PaymentStatus status,

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Schema(description = "조회 시작일", example = "2026-01-01")
            LocalDate from,

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Schema(description = "조회 종료일", example = "2026-02-01")
            LocalDate to,

            @Schema(description = "마지막으로 조회된 결제 ID (커서)", example = "150")
            Long lastId,

            @Positive
            @Schema(description = "페이지 당 조회 건수", example = "20", defaultValue = "10")
            int size
    ) {}

    @Schema(description = "가맹점 매출 요약 응답")
    public record SalesSummaryResponse(
            @Schema(description = "총 매출액 (성공한 결제 합계)", example = "1500000")
            Long totalSalesAmount,

            @Schema(description = "총 거래 건수", example = "120")
            Long totalTransactionCount,

            @Schema(description = "총 환불액", example = "50000")
            Long totalRefundAmount,

            @Schema(description = "순 매출액 (총 매출액 - 총 환불액)", example = "1450000")
            Long netAmount
    ) {}

    @Schema(description = "가맹점 결제 내역 응답")
    public record PaymentHistoryResponse(
            @Schema(description = "결제 외부 식별자 (UUID)", example = "pay-abcd-1234")
            String externalPaymentId,

            @Schema(description = "결제 금액", example = "12000")
            Long amount,

            @Schema(description = "결제 상태", example = "SUCCESS")
            PaymentStatus status,

            @Schema(description = "결제 일시")
            LocalDateTime createdAt,

            @Schema(description = "마스킹된 카드 번호", example = "9410-****-****-1234")
            String cardMaskedNumber
    ) {}
}