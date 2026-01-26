package com.m2nsteel.bank_program_modernization.controller;

import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.response.BranchResponse;
import com.m2nsteel.bank_program_modernization.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchCreateRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }
}