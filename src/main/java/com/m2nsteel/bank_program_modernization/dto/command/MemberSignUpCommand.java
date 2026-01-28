package com.m2nsteel.bank_program_modernization.dto.command;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record MemberSignUpCommand(
        String loginId,
        String password,
        String name,
        String contact
) {}