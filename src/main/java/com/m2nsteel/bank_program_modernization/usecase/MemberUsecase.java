package com.m2nsteel.bank_program_modernization.usecase;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

public class MemberUsecase {
    public record AdminSignUpCommand(
            String loginId,
            String password,
            String name,
            String contact,
            String department
    ) {}

    public record AdminUpdateCommand(
            @Nullable String password,
            @Nullable String name,
            @Nullable String contact,
            @Nullable String department
    ) {}

    public record MemberSignUpCommand(
            String loginId,
            String password,
            String name,
            String contact
    ) {}

    public record MemberUpdateCommand(
            @Nullable String password,
            @Nullable String name,
            @Nullable String contact
    ) {}

    public record MerchantSignUpCommand(
            String loginId,
            String password,
            String name,
            String contact,
            String businessNumber,
            String shopName,
            String category
    ) {}
    public record MerchantUpdateCommand(
            @Nullable String password,
            @Nullable String name,
            @Nullable String contact,
            @Nullable String shopName,
            @Nullable String category
    ) {}

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

    public record MemberResult(
            String externalId,
            String loginId,
            String name,
            String contact,
            MemberRole role,
            MemberStatus status,
            LocalDateTime createdAt
    ) {}

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

}
