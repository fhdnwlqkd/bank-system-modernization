package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawRequest(
        @NotNull(message = "출금 금액은 필수입니다.")
        @Positive(message = "0원 이하의 금액은 출금할 수 없습니다.")
        Long amount,

        @NotBlank(message = "출금 비밀번호는 필수입니다.")
        String accountPassword
) {
}
