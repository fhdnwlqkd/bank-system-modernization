package com.m2nsteel.bank_program_modernization.dto.command;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record MerchantSignUpCommand(
        String loginId,
        String password,
        String name,
        String contact,
        String businessNumber,
        String shopName,
        String category
) {}
