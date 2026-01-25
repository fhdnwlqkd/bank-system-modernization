package com.m2nsteel.bank_program_modernization.dto.response;

public record AccountResponse (
    Long memberId,
    String accountNumber,
    Long balance,
    String status
) {}
