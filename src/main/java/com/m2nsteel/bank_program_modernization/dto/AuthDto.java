package com.m2nsteel.bank_program_modernization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthDto {
    @Schema(description = "로그인 요청")
    public record LoginRequest(
            @Schema(description = "로그인 ID", example = "gildong123")
            @NotBlank String loginId,

            @Schema(description = "비밀번호", example = "Password123!")
            @NotBlank String password
    ) {}

    @Schema(description = "인증 토큰 응답")
    public record TokenResponse(
            @Schema(description = "Access 토큰 (Bearer 타입, 만료 시간 1시간)",
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            String accessToken,

            @Schema(description = "Refresh 토큰 (토큰 재발급용, 만료 시간 2주)",
                    example = "defGhiJklMnoPqrStuVwxYz123456...")
            String refreshToken
    ) {}
}
