package com.m2nsteel.bank_program_modernization.dto.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record MemberUpdateCommand(
        @Nullable String password,
        @Nullable String name,
        @Nullable String contact
) {}
