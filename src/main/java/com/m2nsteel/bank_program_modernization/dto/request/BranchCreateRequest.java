package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchCreateRequest (
        @NotBlank(message = "지점명은 필수입니다.")
        String name,
        @NotBlank(message = "지점 주소는 필수입니다.")
        String address,
        String contact
){}
