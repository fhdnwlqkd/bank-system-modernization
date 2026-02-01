package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.controller.mapper.CardDtoMapper;
import com.m2nsteel.bank_program_modernization.core.api.ApiResponse;
import com.m2nsteel.bank_program_modernization.dto.CardDto;
import com.m2nsteel.bank_program_modernization.service.CardService;
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

@Tag(name = "Card", description = "카드 관리 및 결제 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardDtoMapper cardMapper;

    /**
     * 카드 신규 발급
     */
    @Operation(summary = "카드 발급", description = "인증된 사용자의 특정 계좌와 연결된 신규 카드를 발급합니다.")
    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<CardDto.CardResponse>> issueCard(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @Valid @RequestBody CardDto.CardIssueRequest request
    ) {
        // 1. DTO -> Command 변환
        var command = cardMapper.toCommand(request);

        // 2. 서비스 호출
        var result = cardService.issueCard(command, memberExternalId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardMapper.from(result)));
    }

    /**
     * 특정 카드 상세 조회
     */
    @Operation(summary = "카드 상세 조회", description = "카드 식별자를 통해 카드 정보를 조회합니다.")
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<ApiResponse<CardDto.CardResponse>> getCardDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @PathVariable("cardId") String cardExternalId
    ) {
        var result = cardService.getCardDetail(cardExternalId, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(cardMapper.from(result)));
    }

    /**
     * 내 모든 카드 목록 조회
     */
    @Operation(summary = "내 카드 목록 조회", description = "현재 로그인한 사용자가 보유한 모든 카드 목록을 조회합니다.")
    @GetMapping("/members/me/cards")
    public ResponseEntity<ApiResponse<List<CardDto.CardResponse>>> getMyCards(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId
    ) {
        var results = cardService.getMyCards(memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(
                results.stream().map(cardMapper::from).toList()
        ));
    }

    /**
     * 카드 결제 처리
     */
    @Operation(summary = "카드 결제", description = "카드 번호와 비밀번호를 이용하여 상점에서 결제를 진행합니다.")
    @PostMapping("cards/pay")
    public ResponseEntity<ApiResponse<CardDto.CardPaymentResponse>> pay(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "username") String memberExternalId,
            @Valid @RequestBody CardDto.CardPaymentRequest request
    ) {
        // DTO를 서비스 커맨드로 변환
        var command = cardMapper.toCommand(request);

        // 인증된 사용자의 식별자를 함께 전달하여 소유권 검증 수행
        var result = cardService.pay(command, memberExternalId);

        return ResponseEntity.ok(ApiResponse.success(cardMapper.from(result)));
    }

    /**
     * 카드 결제 환불 처리
     */
    @Operation(summary = "결제 환불", description = "결제 식별자를 통해 승인된 결제를 환불 처리합니다.")
    @PostMapping("cards/refund")
    public ResponseEntity<ApiResponse<CardDto.RefundResponse>> refund(
            @Valid @RequestBody CardDto.RefundRequest request
    ) {
        // 서비스 레이어의 환불 로직 호출
        var command = cardMapper.toCommand(request);
        var result = cardService.refund(command);

        return ResponseEntity.ok(ApiResponse.success(cardMapper.from(result)));
    }
}