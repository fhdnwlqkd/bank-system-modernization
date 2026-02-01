package com.m2nsteel.bank_program_modernization.dto;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class MemberDto {
    @Schema(description = "일반 회원가입 요청")
    public record MemberSignUpRequest(
            @Schema(description = "로그인 ID", example = "gildong123")
            @NotBlank @Size(min = 4, max = 20)
            String loginId,

            @Schema(description = "비밀번호 (특수문자 포함 8자 이상)", example = "Password123!")
            @NotBlank @Size(min = 8)
            String password,

            @Schema(description = "사용자 실명", example = "홍길동")
            @NotBlank
            String name,

            @Schema(description = "연락처 (하이픈 포함)", example = "010-1234-5678")
            @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$")
            String contact
    ) {}

    @Schema(description = "가맹점 회원가입 요청")
    public record MerchantSignUpRequest(
            @Schema(description = "로그인 ID", example = "cafe_boss")
            @NotBlank String loginId,

            @Schema(description = "비밀번호", example = "CafePass123!")
            @NotBlank String password,

            @Schema(description = "가맹점 전용 계좌 비밀번호 (4자리)", example = "1234")
            @NotBlank @Pattern(regexp = "^\\d{4}$")
            String accountPassword,

            @Schema(description = "대표자 성함", example = "홍길동")
            @NotBlank String name,

            @Schema(description = "연락처", example = "010-5555-6666")
            @NotBlank String contact,

            @Schema(description = "사업자 등록번호", example = "123-45-67890")
            @NotBlank @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$")
            String businessNumber,

            @Schema(description = "가맹점 명칭", example = "길동네 카페")
            @NotBlank String merchantName,

            @Schema(description = "업종 카테고리", example = "FOOD_CAFE")
            @NotBlank String category
    ) {}

    @Schema(description = "관리자 회원가입 요청")
    public record AdminSignUpRequest(
            @Schema(description = "관리자 로그인 ID", example = "admin_root")
            @NotBlank String loginId,

            @Schema(description = "관리자 비밀번호", example = "AdminSecure!00")
            @NotBlank String password,

            @Schema(description = "관리자 성함", example = "관리자A")
            @NotBlank String name,

            @Schema(description = "비상 연락처", example = "010-9999-8888")
            @NotBlank String contact,

            @Schema(description = "소속 부서", example = "IT_SECURITY")
            @NotBlank String department
    ) {}

    @Schema(description = "일반 회원가입 응답")
    public record MemberResponse(
            @Schema(description = "외부 노출용 식별자", example = "mem_8f2b3c4d")
            String externalId,

            @Schema(description = "로그인 ID", example = "gildong123")
            String loginId,

            @Schema(description = "사용자 이름", example = "홍길동")
            String name,

            @Schema(description = "연락처", example = "010-1234-5678")
            String contact,

            @Schema(description = "계정 상태", example = "ACTIVE")
            MemberStatus status,

            @Schema(description = "가입 일시", example = "2026-02-01T14:00:00")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "가맹점 회원가입 응답")
    public record MerchantSignUpResponse(
            @Schema(description = "외부 노출용 식별자", example = "mer_9a1b2c3d")
            String externalId,

            @Schema(description = "로그인 ID", example = "cafe_boss")
            String loginId,

            @Schema(description = "대표자 이름", example = "홍길동")
            String name,

            @Schema(description = "연락처", example = "010-5555-6666")
            String contact,

            @Schema(description = "사업자 등록번호", example = "123-45-67890")
            String businessNumber,

            @Schema(description = "자동 생성된 정산 계좌번호", example = "123-456-789012")
            String accountNumber,

            @Schema(description = "가맹점명", example = "길동네 카페")
            String merchantName,

            @Schema(description = "업종 카테고리", example = "FOOD_CAFE")
            String category,

            @Schema(description = "계정 상태", example = "ACTIVE")
            MemberStatus status,

            @Schema(description = "가입 일시", example = "2026-02-01T14:00:00")
            LocalDateTime createdAt
    ) {}

    @Schema(description = "관리자 회원가입 응답")
    public record AdminResponse(
            @Schema(description = "외부 노출용 식별자", example = "adm_1a2b3c4d")
            String externalId,

            @Schema(description = "로그인 ID", example = "admin_root")
            String loginId,

            @Schema(description = "관리자 이름", example = "관리자A")
            String name,

            @Schema(description = "비상 연락처", example = "010-9999-8888")
            String contact,

            @Schema(description = "소속 부서", example = "IT_SECURITY")
            String department,

            @Schema(description = "계정 상태", example = "ACTIVE")
            MemberStatus status,

            @Schema(description = "가입 일시", example = "2026-02-01T14:00:00")
            LocalDateTime createdAt
    ) {}
}
