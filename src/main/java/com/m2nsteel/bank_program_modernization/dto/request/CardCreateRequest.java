package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CardCreateRequest(
        @NotNull(message = "연결할 계좌번호는 필수입니다.")
        String accountNumber,

        @NotBlank(message = "카드 비밀번호는 필수입니다.")
        @Pattern(regexp = "^\\d{4}$", message = "카드 비밀번호는 숫자 4자리여야 합니다.")
        String cardPassword,

        @NotNull(message = "카드 종류는 필수입니다.")
        String cardType // CHECK, CREDIT 등
) {
}
