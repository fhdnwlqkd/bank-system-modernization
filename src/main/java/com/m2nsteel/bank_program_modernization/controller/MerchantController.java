package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.MerchantDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.MerchantDto;
import com.m2nsteel.bank_program_modernization.service.MerchantSalesService;
import com.m2nsteel.bank_program_modernization.usecase.CursorResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Merchant", description = "가맹점 전용 API")
@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantSalesService merchantService;
    private final MerchantDtoMapper merchantMapper;

    /**
     * 기간별 정산/매출 요약 조회
     */
    @Operation(summary = "정산 요약 조회", description = "지정한 기간 동안의 가맹점 매출 합계 및 건수를 조회합니다.")
    @GetMapping("/me/settlements")
    public ResponseEntity<ApiResponse<MerchantDto.SalesSummaryResponse>> getSettlementSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // 서비스 로직 호출: 가맹점주의 memberExternalId를 사용하여 소유권 확인 포함
        var result = merchantService.getSettlementSummary(externalId, from, to);

        return ResponseEntity.ok(ApiResponse.success(merchantMapper.from(result)));
    }

    /**
     * 가맹점 결제 내역 조회 (커서 페이징)
     */
    @Operation(summary = "결제 내역 조회", description = "가맹점에 발생한 결제 내역을 커서 기반 페이징으로 조회합니다.")
    @GetMapping("/me/payments")
    public ResponseEntity<ApiResponse<CursorResult<MerchantDto.PaymentHistoryResponse>>> getPayments(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String externalId,
            @Valid MerchantDto.MerchantPaymentSearchRequest request
    ) {
        // DTO를 SearchCondition으로 변환
        var condition = merchantMapper.toCondition(request, externalId);

        // 커서 페이징 조회 수행
        var result = merchantService.getPayments(externalId, condition);

        // 결과 DTO 매핑 (CursorResult 내부의 List 변환)
        var responseValues = result.values().stream()
                .map(merchantMapper::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new CursorResult<>(
                responseValues,
                result.nextCursor(),
                result.hasNext()
        )));
    }
}