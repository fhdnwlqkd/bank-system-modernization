package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotNull;

public record AccountCreateRequest (
        @NotNull Long memberId,
        @NotNull Long branchId,
        @NotNull String accountPassword
) {}
