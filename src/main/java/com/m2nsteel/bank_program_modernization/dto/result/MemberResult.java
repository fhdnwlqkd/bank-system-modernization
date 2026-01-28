package com.m2nsteel.bank_program_modernization.dto.result;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;

import java.time.LocalDateTime;

public record MemberResult(
        String externalId,
        String loginId,
        String name,
        String contact,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt
) {}
