package com.m2nsteel.bank_program_modernization.usecase;

import jakarta.validation.constraints.NotBlank;

public class AuthUsecase {
    public record LoginCommand(
            @NotBlank String loginId,
            @NotBlank String password
    ) {}

    public record TokenResult(
            String accessToken,
            String refreshToken
    ) {}
}
