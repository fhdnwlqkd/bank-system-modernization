package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
        @NotBlank String requestId,      // 중복 요청 방지용 (Idempotency Key)
        @NotNull String accountNumber,
        @NotNull(message = "입금 금액은 필수입니다.")
        @Positive(message = "0원 이하의 금액은 입금할 수 없습니다.")
        Long amount
) {
}
