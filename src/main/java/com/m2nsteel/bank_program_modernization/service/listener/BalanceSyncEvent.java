package com.m2nsteel.bank_program_modernization.service.listener;

public record BalanceSyncEvent(
        Long accountId,
        Long balance
) {}
