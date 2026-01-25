package com.m2nsteel.bank_program_modernization.dto.response;

public record BranchResponse(
        Long id,
        String name,
        String branchCode,
        String address,
        String contact
) {
}
