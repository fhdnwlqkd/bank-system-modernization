package com.m2nsteel.bank_program_modernization.dto.result;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;

import java.time.LocalDateTime;

public record MerchantResult(
        String externalId,
        String loginId,
        String name,
        String contact,
        String businessNumber,
        String shopName,
        String category,
        MemberStatus status,
        LocalDateTime createdAt
) {}
