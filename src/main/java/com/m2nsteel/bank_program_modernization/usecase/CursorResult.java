package com.m2nsteel.bank_program_modernization.usecase;

import java.util.List;

public record CursorResult<T>(
        List<T> values,
        Long nextCursor,
        boolean hasNext
) {}