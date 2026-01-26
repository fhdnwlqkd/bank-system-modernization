package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CardPaymentRequest(
        @NotBlank String requestId,      // 중복 요청 방지용 (Idempotency Key)
        @NotBlank(message = "카드 번호는 필수입니다.")
        String cardNumber,

        @NotBlank(message = "카드 비밀번호는 필수입니다.")
        @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자입니다.")
        String cardPassword,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount,

        @NotBlank(message = "가맹점 사업자 번호는 필수입니다.")
        String businessRegistrationNumber
) {
}
