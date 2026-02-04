package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class AccountQueryService {

    private final RedisAccountService redisAccountService;

    /**
     * 계좌번호로 조회: Redis의 번호-ID 매핑을 이용해 DB 접근 없이 객체를 광속으로 반환합니다. 
     */
    public Account getAccountByNumber(String accountNumber) {
        return redisAccountService.getAccountByNumber(accountNumber);
    }

    /**
     * 회원 객체로 계좌 조회: Redis의 회원-계좌 관계 인덱스(Set)를 이용해 즉시 조회합니다. 
     */
    public Account getAccountByMember(Member member) {
        // Redis에서 해당 회원의 계좌 목록을 가져옵니다 
        List<Account> accounts = redisAccountService.getAccountsByMember(member);

        if (accounts.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // 여러 계좌 중 첫 번째(대표 계좌)를 반환합니다 
        return accounts.getFirst();
    }

    /**
     * 회원의 모든 계좌 조회: DB 접근 없이 Redis에서 직접 리스트를 구성합니다. 
     */
    public List<Account> findAllByMember(Member member) {
        return redisAccountService.getAccountsByMember(member);
    }
}