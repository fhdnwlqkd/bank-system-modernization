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

    /**
     * ======================
     * Member APIs
     * ======================
     */
    @GetMapping("/members/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String loginId = userDetails.getUsername();
        return ResponseEntity.ok(memberService.getMemberInfo(loginId));
    }

    @GetMapping("members/me/accounts")
    public void getMyAccounts() {

    }

    @GetMapping("/members/me/cards")
    public void getMyCards() {

    }

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody MemberSignUpRequest request) {
        return ResponseEntity.ok(memberService.signUp(request));
    }

    @DeleteMapping("/members/me")
    public void deleteMember() {

    }

    /**
     * ======================
     * Merchant APIs
     * ======================
     */
    @GetMapping("/merchants/me")
    public void getMerchantInfo() {

    }

    @PostMapping("/merchants")
    public ResponseEntity<MerchantSignUpResponse> signUpMerchant(@Valid @RequestBody MerchantSignUpRequest request) {
        return ResponseEntity.ok(memberService.merchantSignUp(request));
    }

    @PatchMapping("/merchants/me")
    public void updateMerchantInfo() {

    }

    @DeleteMapping("/merchants/me")
    public void deleteMerchant() {

    }

    /**
    * ======================
    * Admin APIs
    * ======================
    */
    @GetMapping("/members")
    public void getAllMembers() {

    }

    @GetMapping("/merchants")
    public void getAllMerchants() {

    }

    @GetMapping("/members/{memberNumber}")
    public void getMemberDetails(@PathVariable String memberNumber) {

    }

    @GetMapping("/members/{memberNumber}/status")
    public void getMemberStatus(@PathVariable String memberNumber) {

    }
}
