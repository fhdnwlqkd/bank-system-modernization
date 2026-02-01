package com.m2nsteel.bank_program_modernization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;

@NullMarked
@Schema(description = "계좌 관련 데이터 전송 객체 (DTO)")
public class AccountDto {

    @Schema(description = "계좌 생성 요청")
    public record AccountCreateRequest(
            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 숫자 4자리여야 합니다.")
            @Schema(description = "계좌 비밀번호 (숫자 4자리)", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String accountPassword
    ) {}

    @Schema(description = "계좌 비밀번호 변경 요청")
    public record AccountChangePasswordRequest(
            @NotBlank
            @Schema(description = "기존 비밀번호", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
            String password,

            @NotBlank
            @Schema(description = "새로운 비밀번호", example = "5678", requiredMode = Schema.RequiredMode.REQUIRED)
            String newPassword
    ) {}

    @Schema(description = "계좌 정보 응답")
    public record AccountResponse(
            @Schema(description = "계좌 고유 식별자 (UUID)", example = "f47ac...")
            String externalId,

            @Schema(description = "계좌 번호", example = "123-456-789012")
            String accountNumber,

            @Schema(description = "현재 잔액 (단위: 원)", example = "1000000")
            Long balance,

            @Schema(description = "계좌 상태", example = "ACTIVE")
            String status,

            @Schema(description = "계좌 생성 일시", example = "2026-02-01T21:15:28")
            LocalDateTime createdAt
    ) {}
}