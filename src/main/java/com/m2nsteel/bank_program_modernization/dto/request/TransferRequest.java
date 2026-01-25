package com.m2nsteel.bank_program_modernization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest (
    @NotBlank String requestId,
    @NotBlank String fromAccountNumber,
    @NotBlank String toAccountNumber,
    @NotBlank String accountPassword,
    @NotNull @Positive Long amount
) {}