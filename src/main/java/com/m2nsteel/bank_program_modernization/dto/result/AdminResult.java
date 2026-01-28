package com.m2nsteel.bank_program_modernization.dto.result;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;

import java.time.LocalDateTime;

public record AdminResult(
        String externalId,
        String loginId,
        String name,
        String contact,
        String department,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt
) {}
