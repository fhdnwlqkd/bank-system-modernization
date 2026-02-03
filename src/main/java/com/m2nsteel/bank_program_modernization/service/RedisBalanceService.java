package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisBalanceService {
    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private static final String ACCOUNT_BALANCE_KEY = "acc:bal:";

    /**
     * 출금: 잔액 차감 로직
     * @return 차감 후 잔액 (잔액 부족 시 -1 반환)
     */
    public Long decreaseBalance(Long accountId, Long amount) {
        String key = ACCOUNT_BALANCE_KEY + accountId;

        // 1. 레디스에서 차감 실행
        Long remain = redisTemplate.opsForValue().decrement(key, amount);

        // 2. [안전장치] 만약 잔액이 0보다 작아졌다면
        if (remain != null && remain < 0) {
            // 다시 원래대로 복구(Rollback) 시키고 -1 반환
            redisTemplate.opsForValue().increment(key, amount);
            return -1L;
        }
        return remain;
    }

    /**
     * 입금: 잔액 증액 로직
     */
    public Long increaseBalance(Long accountId, Long amount) {
        String key = ACCOUNT_BALANCE_KEY + accountId;
        return redisTemplate.opsForValue().increment(key, amount);
    }

    /**
     * 조회: 현재 실시간 잔액 가져오기
     */
    public Long getBalance(Long accountId) {
        String key = ACCOUNT_BALANCE_KEY + accountId;
        String val = redisTemplate.opsForValue().get(key);
        if(val == null){
            log.info("[Redis Miss] DB에서 잔액을 가져옵니다. accountId: {}", accountId);
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
            redisTemplate.opsForValue().set(key, String.valueOf(account.getBalance()));
            return account.getBalance();
        }
        log.info("[Redis Hit] Redis에서 잔액을 가져옵니다. accountId: {}", accountId);
        return Long.parseLong(val);
    }
}
