package com.m2nsteel.bank_program_modernization.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.service.listener.BalanceSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisAccountService {
    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final String OBJ_KEY = "acc:obj:";      // ID -> Account 객체
    private static final String NUM_KEY = "acc:num:";      // 계좌번호 -> ID 매핑
    private static final String MEM_ACCS_KEY = "mem:accs:"; // 회원ID -> 계좌ID 리스트(Set)

    /** 1. 회원 객체로 모든 계좌 조회 (DB 0회 접근! ) */
    public List<Account> getAccountsByMember(Member member) {
        String key = MEM_ACCS_KEY + member.getId();
        Set<String> accountIds = redisTemplate.opsForSet().members(key);

        if (accountIds == null || accountIds.isEmpty()) {
            log.info("[Redis Member Miss] DB에서 회원의 계좌 목록을 조회합니다. Member: {}", member.getId());
            List<Account> accounts = accountRepository.findAllByMember(member);
            accounts.forEach(acc -> saveAccount(acc, member));
            return accounts;
        }

        return accountIds.stream()
                .map(id -> getAccount(Long.valueOf(id)))
                .collect(Collectors.toList());
    }

    /** 2. 계좌번호로 조회  */
    public Account getAccountByNumber(String accountNumber) {
        String mapKey = NUM_KEY + accountNumber;
        String accountId = redisTemplate.opsForValue().get(mapKey);

        if (accountId == null) {
            log.info("[Redis Map Miss] DB에서 조회: {}", accountNumber);
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
            // 객체 내의 Member 정보를 사용하여 캐싱 처리 
            saveAccount(account, account.getMember());
            return account;
        }

        return getAccount(Long.valueOf(accountId));
    }

    /** 3. ID로 객체 조회 (내부용)  */
    public Account getAccount(Long accountId) {
        String key = OBJ_KEY + accountId;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
            saveAccount(account, account.getMember());
            return account;
        }

        try {
            return objectMapper.readValue(json, Account.class);
        } catch (Exception e) {
            log.error("역직렬화 실패 ", e);
            throw new RuntimeException(e);
        }
    }

    /** 4. 저장: 객체, 번호매핑, 회원관계를 한 번에 저장합니다  */
    public void saveAccount(Account account, Member member) {
        try {
            String accountIdStr = String.valueOf(account.getId());
            String json = objectMapper.writeValueAsString(account);

            // 1. 객체 JSON 저장
            redisTemplate.opsForValue().set(OBJ_KEY + accountIdStr, json);
            // 2. 계좌번호-ID 매핑 저장
            redisTemplate.opsForValue().set(NUM_KEY + account.getAccountNumber(), accountIdStr);
            // 3. 회원-계좌 관계 인덱스 저장 (Redis Set)
            redisTemplate.opsForSet().add(MEM_ACCS_KEY + member.getId(), accountIdStr);

        } catch (Exception e) {
            log.error("직렬화 실패 ", e);
        }
    }

    /** 5. 업데이트: Redis 우선 반영 후 비동기 DB 동기화 이벤트를 발행합니다  */
    @Transactional
    public Long updateBalance(Long accountId, Long amount, boolean isIncrease) {
        Account account = getAccount(accountId);

        // 도메인 로직 수행 (잔액 증감) 
        if (isIncrease) {
            account.deposit(amount);
        } else {
            if (account.getBalance() < amount) return -1L; // 잔액 부족 
            account.withdraw(amount);
        }

        // Redis 상태 즉시 업데이트 
        saveAccount(account, account.getMember());
        eventPublisher.publishEvent(new BalanceSyncEvent(account.getId(), account.getBalance()));

        return account.getBalance();
    }
}