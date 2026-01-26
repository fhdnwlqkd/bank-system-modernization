package com.m2nsteel.bank_program_modernization.dto.response;

public record MerchantSignUpResponse(
        Long memberId,
        String loginId,
        String memberNumber,
        String merchantName,
        String businessRegistrationNumber,
        String merchantCategory
) {}
