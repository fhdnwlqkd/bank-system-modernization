package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.AccountDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.AccountDto;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Account", description = "계좌 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountDtoMapper accountMapper;

    /**
     * 신규 계좌 개설
     */
    @Operation(summary = "계좌 개설", description = "인증된 사용자의 신규 계좌를 생성합니다.")
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AccountDto.AccountResponse>> createAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId,
            @Valid @RequestBody AccountDto.AccountCreateRequest request
    ) {
        // 1. DTO -> Command 변환
        var command = accountMapper.toCommand(request, externalId);

        // 2. 서비스 호출 및 결과 획득
        var result = accountService.createAccount(command);

        // 3. 201 Created 응답 리턴
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountMapper.from(result)));
    }

    /**
     * 내 계좌 목록 조회
     */
    @Operation(summary = "내 계좌 목록 조회", description = "현재 로그인한 사용자가 보유한 모든 계좌를 조회합니다.")
    @GetMapping("/members/me/accounts")
    public ResponseEntity<ApiResponse<List<AccountDto.AccountResponse>>> getMyAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId
    ) {
        var results = accountService.getMyAccounts(externalId);

        return ResponseEntity.ok(ApiResponse.success(
                results.stream().map(accountMapper::from).toList()
        ));
    }

    /**
     * 특정 계좌 상세 조회
     */
    @Operation(summary = "계좌 상세 조회", description = "계좌 식별자를 통해 특정 계좌의 상세 정보를 조회합니다.")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<AccountDto.AccountResponse>> getAccountDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId,
            @PathVariable("accountId") String accountExternalId
    ) {
        var result = accountService.getAccountDetail(accountExternalId, externalId);

        return ResponseEntity.ok(ApiResponse.success(accountMapper.from(result)));
    }

    /**
     * 계좌 비밀번호 변경
     */
    @Operation(summary = "계좌 비밀번호 변경", description = "기존 비밀번호 확인 후 새로운 비밀번호로 변경합니다.")
    @PatchMapping("accounts/{accountExternalId}/password")
    public ResponseEntity<ApiResponse<AccountDto.AccountResponse>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @PathVariable("accountExternalId") String accountExternalId, // 이름 명시 ㅋ
            @Valid @RequestBody AccountDto.AccountChangePasswordRequest request
    ) {
        // 1. DTO -> Command 변환
        var command = accountMapper.toCommand(request);

        // 2. 서비스 호출
        var result = accountService.changePassword(command, accountExternalId, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(accountMapper.from(result)));
    }

    /**
     * 계좌 해지 (정지)
     */
    @Operation(summary = "계좌 해지", description = "보유한 계좌를 해지 상태로 변경합니다.")
    @DeleteMapping("accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> closeAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @PathVariable("accountId") String accountExternalId
    ) {
        accountService.close(accountExternalId, memberExternalId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}