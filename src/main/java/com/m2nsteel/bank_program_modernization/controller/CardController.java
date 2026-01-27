package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.dto.request.CardCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.CardPaymentRequest;
import com.m2nsteel.bank_program_modernization.dto.response.CardCreateResponse;
import com.m2nsteel.bank_program_modernization.dto.response.CardPaymentResponse;
import com.m2nsteel.bank_program_modernization.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardCreateResponse> createCard(
            @Valid @RequestBody CardCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.createCard(request, userDetails.getUsername()));
    }

    @PostMapping("/payments")
    public ResponseEntity<CardPaymentResponse> processPayment(@Valid @RequestBody CardPaymentRequest request) {
        return ResponseEntity.ok(cardService.pay(request));
    }

}
