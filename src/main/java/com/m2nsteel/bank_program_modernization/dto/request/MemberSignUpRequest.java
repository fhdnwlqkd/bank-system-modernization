package com.m2nsteel.bank_program_modernization.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberSignUpRequest(
        @NotBlank(message = "로그인 ID는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password,
        @NotBlank(message = "고객번호는 필수입니다.") String MemberNumber,
        @NotBlank(message = "이름은 필수입니다.") String name,
        @NotNull Long branchId
) {}
