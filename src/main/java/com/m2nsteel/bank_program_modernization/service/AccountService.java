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
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator generator;

    /**
     * 신규 계좌 개설
     */
    @Transactional
    public AccountUsecase.AccountResult createAccount(AccountUsecase.AccountCreateCommand command) {
        // 1. 회원 조회 (externalId 기준)
        Member member = memberRepository.findByExternalId(command.memberExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if(!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        // 2. 계좌번호 생성
        String accountNumber = generator.generate();

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.accountPassword());

        // 3. 계좌 엔티티 생성
        Account account = Account.create(
                accountNumber,
                encodedPassword,
                member
        );

        return accountMapper.toResult(accountRepository.save(account));
    }

    /**
     * 계좌 조회
     */
    public List<AccountUsecase.AccountResult> getMyAccounts(String memberExternalId) {
        Member member = memberRepository.findByExternalId(memberExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // Member의 externalId로 연관된 모든 계좌를 조회합니다.
        return accountRepository.findAllByMember(member)
                .stream()
                .map(accountMapper::toResult)
                .toList();
    }

    /**
     * 특정 계좌 상세 조회
     */
    public AccountUsecase.AccountResult getAccountDetail(String accountExternalId, String memberExternalId) {
        // 1. 계좌 및 회원 조회
        Member member = memberRepository.findByExternalId(memberExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Account account = accountRepository.findByExternalIdAndMember(accountExternalId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return accountMapper.toResult(account);
    }

    /**
     * 계좌 비밀번호 변경
     */
    @Transactional
    public AccountUsecase.AccountResult changePassword(AccountUsecase.AccountChangePasswordCommand command, String accountExternalId, String memberExternalId) {
        // 1. 계좌 및 회원 조회
        Account account = findMyAccount(accountExternalId, memberExternalId);
        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(command.password(), account.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        // 3. 비밀번호 변경
        account.changePassword(passwordEncoder.encode(command.newPassword()));
        return accountMapper.toResult(account);
    }

    /**
     * 계좌 정지
     */
    @Transactional
    public AccountUsecase.AccountResult close(String accountExternalId, String memberExternalId) {
        // 1. 계좌 및 회원 조회
        Account account = findMyAccount(accountExternalId, memberExternalId);
        account.close();
        return accountMapper.toResult(account);
    }

    private Account findMyAccount(String accountExternalId, String memberExternalId) {
        Member member = memberRepository.findByExternalId(memberExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return accountRepository.findByExternalIdAndMember(accountExternalId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}