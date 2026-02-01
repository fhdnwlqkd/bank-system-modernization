package com.m2nsteel.bank_program_modernization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
}