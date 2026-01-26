package com.m2nsteel.bank_program_modernization.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
