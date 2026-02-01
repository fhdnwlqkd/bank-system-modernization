package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.TransactionDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.TransactionDto;
import com.m2nsteel.bank_program_modernization.service.TransactionSearchService;
import com.m2nsteel.bank_program_modernization.service.TransactionService;
import com.m2nsteel.bank_program_modernization.usecase.CursorResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Tag(name = "Transaction", description = "거래 관리 API (입금/출금/이체)")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionSearchService transactionSearchService;
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

    /**
     * 내 계좌 거래 내역 조회 (페이징 & 동적 검색)
     */
    @Operation(
            summary = "거래 내역 조회",
            description = "권한과 ID를 직접 추출하여 처리합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<CursorResult<TransactionDto.TransactionHistoryResponse>>> getTransactionHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid TransactionDto.TransactionSearchRequest query,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 1. 객체 내부에서 필요한 정보 추출
        String memberExternalId = userDetails.getUsername();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        // 3. 서비스 호출
        var result = transactionSearchService.searchTransactions(
                memberExternalId,
                isAdmin,
                transactionMapper.toCondition(query),
                pageable
        );

        // 4. 결과 변환 및 리턴
        List<TransactionDto.TransactionHistoryResponse> responseValues = result.values().stream()
                .map(transactionMapper::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new CursorResult<>(
                responseValues,
                result.nextCursor(),
                result.hasNext()
        )));
    }
}