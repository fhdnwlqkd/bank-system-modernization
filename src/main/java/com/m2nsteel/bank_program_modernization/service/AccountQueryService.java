package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountQueryService {
    private final AccountRepository accountRepository;
    private final RedisBalanceService redisBalanceService;

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.updateBalanceForView(redisBalanceService.getBalance(account.getId()));
        return account;
    }

    @Transactional(readOnly = true)
    public Account getAccountByMember(Member member) {
        return accountRepository.findByMember(member)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Account> findAllByMember(Member member) {
        // 1. 해당 회원의 모든 계좌를 DB에서 조회
        List<Account> accounts = accountRepository.findAllByMember(member);

        // 2. 각 계좌를 돌면서 Redis 실시간 잔액을 주입
        accounts.forEach(account -> {
            Long realBalance = redisBalanceService.getBalance(account.getId());
            account.updateBalanceForView(realBalance);
        });

        return accounts;
    }
}
