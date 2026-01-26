package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MerchantSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.MemberResponse;
import com.m2nsteel.bank_program_modernization.dto.response.MerchantSignUpResponse;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody MemberSignUpRequest request) {
        return ResponseEntity.ok(memberService.signUp(request));
    }

    @PostMapping("/merchants")
    public ResponseEntity<MerchantSignUpResponse> signUpMerchant(@Valid @RequestBody MerchantSignUpRequest request) {
        return ResponseEntity.ok(memberService.merchantSignUp(request));
    }

    @GetMapping("/members/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String loginId = userDetails.getUsername();
        return ResponseEntity.ok(memberService.getMemberInfo(loginId));
    }
}
