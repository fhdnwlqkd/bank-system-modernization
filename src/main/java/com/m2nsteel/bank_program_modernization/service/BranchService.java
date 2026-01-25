package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.constant.BranchStatus;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.response.BranchResponse;
import com.m2nsteel.bank_program_modernization.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {
    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(BranchCreateRequest request) {
        // 1. 지점 이름 중복 확인
        if (branchRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BRANCH_NAME);
        }

        // 2. 지점 번호 생성
        Long seq = branchRepository.getNextBranchSequence();
        String branchCode = "B-" + seq;

        // 3. 지점 엔티티 생성 및 저장
        Branch branch = Branch.builder()
                .name(request.name())
                .branchCode(branchCode)
                .address(request.address())
                .status(BranchStatus.OPEN)
                .contact(request.contact())
                .createdAt(LocalDateTime.now())
                .build();
        Branch savedBranch = branchRepository.save(branch);

        // 4. 응답 DTO 변환
        return new BranchResponse(
                savedBranch.getId(),
                savedBranch.getName(),
                savedBranch.getBranchCode(),
                savedBranch.getAddress(),
                savedBranch.getContact()
        );
    }
}
