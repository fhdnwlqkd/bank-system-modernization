package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.core.generator.AccountNumberGenerator;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.service.mapper.AccountMapper;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class AccountService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final RedisAccountService redisAccountService;
    private final AccountQueryService accountQueryService;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator generator;

    public List<AccountUsecase.AccountResult> getMyAccounts(String memberExternalId) {
        // 1. Redis에서 회원 목록 조회 시도 
        List<Account> accounts = redisAccountService.getAccountsByMember(memberExternalId);

        if (accounts.isEmpty()) {
            Member member = memberRepository.findByExternalId(memberExternalId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            accounts = accountQueryService.findAllByMember(member);
            accounts.forEach(acc -> redisAccountService.saveAccount(acc, member));
        }

        return accounts.stream().map(accountMapper::toResult).toList();
    }

    public AccountUsecase.AccountResult getAccountDetail(String accountExternalId, String memberExternalId) {
        Account account = findMyAccount(accountExternalId, memberExternalId);
        return accountMapper.toResult(account);
    }

    @Transactional
    public AccountUsecase.AccountResult createAccount(AccountUsecase.AccountCreateCommand command) {
        Member member = memberRepository.findByExternalId(command.memberExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if(!member.isActive()) throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);

        Account account = Account.create(generator.generate(), passwordEncoder.encode(command.accountPassword()), member);
        Account savedAccount = accountRepository.save(account);

        // 캐시 즉시 반영 
        redisAccountService.saveAccount(savedAccount, member);
        return accountMapper.toResult(savedAccount);
    }

    @Transactional
    public AccountUsecase.AccountResult changePassword(AccountUsecase.AccountChangePasswordCommand command, String accountExternalId, String memberExternalId) {
        Account account = findMyAccount(accountExternalId, memberExternalId);
        if (!passwordEncoder.matches(command.password(), account.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        account.changePassword(passwordEncoder.encode(command.newPassword()));

        // 상태 변경 후 캐시 갱신 
        redisAccountService.saveAccount(account, account.getMember());
        return accountMapper.toResult(account);
    }

    @Transactional
    public AccountUsecase.AccountResult close(String accountExternalId, String memberExternalId) {
        Account account = findMyAccount(accountExternalId, memberExternalId);
        account.close();
        redisAccountService.saveAccount(account, account.getMember());
        return accountMapper.toResult(account);
    }

    private Account findMyAccount(String accountExternalId, String memberExternalId) {
        // Redis 우선 조회 
        return redisAccountService.getAccount(accountExternalId)
                .filter(acc -> acc.getMember().getExternalId().equals(memberExternalId))
                .orElseGet(() -> {
                    Member member = memberRepository.findByExternalId(memberExternalId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                    Account dbAccount = accountQueryService.getAccountByMember(member);
                    if (!dbAccount.getExternalId().equals(accountExternalId)) {
                        throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
                    }
                    redisAccountService.saveAccount(dbAccount, member);
                    return dbAccount;
                });
    }
}