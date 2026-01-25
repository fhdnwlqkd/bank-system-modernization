package com.m2nsteel.bank_program_modernization.dto.response;

public record MemberResponse (
    Long memberId,
    String loginId,
    String memberNumber,
    String name
) {}
