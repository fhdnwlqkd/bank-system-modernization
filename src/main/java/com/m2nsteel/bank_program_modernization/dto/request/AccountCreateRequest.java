package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotNull;

public record AccountCreateRequest (
        @NotNull String memberNumber,
        @NotNull String branchCode,
        @NotNull String accountPassword
) {}
