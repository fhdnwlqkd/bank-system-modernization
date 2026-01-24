package com.m2nsteel.bank_program_modernization.core.api;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static ApiResponse<?> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message), LocalDateTime.now());
    }
}
