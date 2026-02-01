package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.TransactionDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.TransactionDto;
import com.m2nsteel.bank_program_modernization.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transaction", description = "거래 관리 API (입금/출금/이체)")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionDtoMapper transactionMapper;

    /**
     * 입금 처리
     */
    @Operation(summary = "입금", description = "내 계좌에 금액을 입금합니다.")
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionDto.GeneralResponse>> deposit(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @Valid @RequestBody TransactionDto.DepositRequest request
    ) {
        var command = transactionMapper.toCommand(request);
        var result = transactionService.deposit(command, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(transactionMapper.from(result)));
    }

    /**
     * 출금 처리
     */
    @Operation(summary = "출금", description = "내 계좌에서 금액을 출금합니다. 계좌 비밀번호가 필요합니다.")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionDto.GeneralResponse>> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @Valid @RequestBody TransactionDto.WithdrawRequest request
    ) {
        var command = transactionMapper.toCommand(request);
        var result = transactionService.withdraw(command, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(transactionMapper.from(result)));
    }

    /**
     * 이체 처리 (계좌 간 송금)
     */
    @Operation(summary = "이체", description = "내 계좌에서 타인 혹은 나의 다른 계좌로 송금합니다.")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionDto.TransferResponse>> transfer(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @Valid @RequestBody TransactionDto.TransferRequest request
    ) {
        var command = transactionMapper.toCommand(request);
        var result = transactionService.transfer(command, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(transactionMapper.from(result)));
    }
}