package com.m2nsteel.bank_program_modernization.core.api;

public record ApiError(
        String code,
        String message
) {}