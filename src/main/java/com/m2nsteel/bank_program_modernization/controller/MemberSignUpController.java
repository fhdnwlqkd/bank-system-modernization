package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.MemberDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Registration", description = "회원가입 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberSignUpController {

    private final MemberService memberService;
    private final MemberDtoMapper memberMapper;

    /**
     * 일반 회원 가입
     */
    @Operation(summary = "일반 회원가입", description = "신규 사용자를 등록합니다.")
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<MemberDto.MemberResponse>> signUp(
            @Valid @RequestBody MemberDto.MemberSignUpRequest request) {

        var command = memberMapper.toCommand(request);
        var result = memberService.signUp(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberMapper.from(result)));
    }

    /**
     * 가맹점 회원 가입
     */
    @Operation(summary = "가맹점 회원가입", description = "신규 가맹점 사용자를 등록합니다.")
    @PostMapping("/merchants")
    public ResponseEntity<ApiResponse<MemberDto.MerchantSignUpResponse>> merchantSignUp(
            @Valid @RequestBody MemberDto.MerchantSignUpRequest request) {

        var command = memberMapper.toCommand(request);
        var result = memberService.merchantSignUp(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberMapper.from(result)));
    }

    /**
     * 관리자 회원 가입
     */
    @Operation(summary = "관리자 회원가입", description = "신규 관리자 사용자를 등록합니다.")
    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<MemberDto.AdminResponse>> adminSignUp(
            @Valid @RequestBody MemberDto.AdminSignUpRequest request) {

        var command = memberMapper.toCommand(request);
        var result = memberService.adminSignUp(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberMapper.from(result)));
    }
}