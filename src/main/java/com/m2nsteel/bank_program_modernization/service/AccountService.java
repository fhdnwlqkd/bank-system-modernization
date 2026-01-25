package com.m2nsteel.bank_program_modernization.service;


import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    /*
    신규 계좌 개설
     */
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {

        // 1. 회원 존재 여부 검증
        if(!memberRepository.existsById(request.memberId())) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 2. 계좌번호 생성
        String accountNumber = generateUniqueAccountNumber();

        // 3. 계좌 엔티티 생성 및 저장
        var account = Account.builder()
                .memberId(request.memberId())
                .accountNumber(accountNumber)
                .branchId(request.branchId())
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
