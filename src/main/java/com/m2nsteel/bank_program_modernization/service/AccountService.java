package com.m2nsteel.bank_program_modernization.service;


import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.BranchRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {
    private final MemberRepository memberRepository;
    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /*
    신규 계좌 개설
     */
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {

        // 1. 회원과 지점을 조회 (검증 + 데이터 확보)
        Member member = memberRepository.findByMemberNumber(request.memberNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Branch branch = branchRepository.findByBranchCode(request.branchCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_NOT_FOUND));

        // 2. 계좌번호 생성 & 계좌 비밀번호 암호화
        String accountNumber = generateUniqueAccountNumber();
        String encodedPassword = passwordEncoder.encode(request.accountPassword());

        // 3. 계좌 엔티티 생성 및 저장
        var account = Account.builder()
                .memberId(member.getId())
                .accountNumber(accountNumber)
                .accountPassword(encodedPassword)
                .branchId(branch.getId())
                .balance(0L)
                .status(AccountStatus.ACTIVE)
                .build();
        var savedAccount = accountRepository.save(account);

        // 4. 응답 DTO 변환
        return new AccountResponse(
                savedAccount.getMemberId(),
                savedAccount.getAccountNumber(),
                savedAccount.getBalance(),
                savedAccount.getStatus().name()
        );
    }

    // 계좌번호 생성 (간단한 예시, 실제로는 더 복잡한 로직 필요)
    // TODO: 중복 일어나지 않도록 개선 필요
    private String generateUniqueAccountNumber() {
        return "110-" + ThreadLocalRandom.current().nextInt(100, 999) + "-" + System.currentTimeMillis() % 1000000;
    }
}
