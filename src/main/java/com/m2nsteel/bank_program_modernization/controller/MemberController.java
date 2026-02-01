package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.MemberDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member - My Page", description = "내 정보 관리 API (인증 기반)")
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberDtoMapper memberMapper;

    /**
     * 내 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 토큰에서 추출하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MemberDto.MemberResponse>> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId
    ) {
        var result = memberService.getMemberInfo(externalId);
        return ResponseEntity.ok(ApiResponse.success(memberMapper.from(result)));
    }

    /**
     * 내 정보 수정
     */
    @Operation(summary = "내 정보 수정", description = "본인의 정보를 수정합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<MemberDto.MemberResponse>> updateMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId,
            @RequestBody @Valid MemberDto.MemberUpdateRequest request
    ) {
        var result = memberService.updateMyInfo(externalId, memberMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.success(memberMapper.from(result)));
    }

    /**
     * 회원 탈퇴
     */
    @Operation(summary = "회원 탈퇴", description = "본인의 계정을 탈퇴 처리합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId
    ) {
        memberService.withdraw(externalId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}