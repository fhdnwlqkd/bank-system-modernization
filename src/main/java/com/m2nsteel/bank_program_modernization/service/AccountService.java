package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
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

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class AccountService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 신규 계좌 개설
     */
    @Transactional
    public AccountUsecase.AccountResult createAccount(AccountUsecase.AccountCreateCommand command) {
        // 1. 회원 및 지점 조회 (externalId 기준)
        Member member = memberRepository.findByExternalId(command.memberExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.accountPassword());

        // 3. 계좌 엔티티 생성 (정적 팩토리 메서드 활용)
        Account account = Account.create(
                encodedPassword,
                member
        );

        return accountMapper.toResult(accountRepository.save(account));
    }
}