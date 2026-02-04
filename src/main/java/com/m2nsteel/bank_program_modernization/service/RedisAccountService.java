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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisAccountService {
    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Redis 키 전략 (네임스페이스 관리) 
    private static final String OBJ_KEY = "acc:obj:";      // ID -> Account JSON 객체
    private static final String NUM_KEY = "acc:num:";      // 계좌번호 -> ID 매핑
    private static final String EXT_KEY = "acc:ext:";      // externalId -> ID 매핑
    private static final String MEM_EXT_KEY = "mem:ext:";  // 회원 externalId -> 회원 ID 매핑
    private static final String MEM_ACCS_KEY = "mem:accs:"; // 회원 ID -> 계좌 ID 리스트(Set)

    /**
     * 1. 회원 객체로 모든 계좌 조회
     */
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** * 2. 회원 externalId로 모든 계좌 조회
     */
    public List<Account> getAccountsByMember(String memberExternalId) {
        String memberId = redisTemplate.opsForValue().get(MEM_EXT_KEY + memberExternalId);

        if (memberId == null) {
            return List.of();
        }

        Set<String> accountIds = redisTemplate.opsForSet().members(MEM_ACCS_KEY + memberId);
        if (accountIds == null || accountIds.isEmpty()) return List.of();

        return accountIds.stream()
                .map(id -> getAccount(Long.valueOf(id)))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 3. 계좌번호로 조회 (Self-Healing 포함)
     */
    public Account getAccountByNumber(String accountNumber) {
        String mapKey = NUM_KEY + accountNumber;
        String accountId = redisTemplate.opsForValue().get(mapKey);

        if (accountId == null) {
            log.info("[Redis Map Miss] DB에서 조회: {}", accountNumber);
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
            saveAccount(account, account.getMember());
            return account;
        }

        return getAccount(Long.valueOf(accountId));
    }

    /**
     * 4. ExternalId로 계좌 단건 조회 (Optional 반환)
     */
    public Optional<Account> getAccount(String externalId) {
        String id = redisTemplate.opsForValue().get(EXT_KEY + externalId);
        return (id == null) ? Optional.empty() : Optional.ofNullable(getAccount(Long.valueOf(id)));
    }

    /**
     * 5. 내부 ID로 객체 조회 (JSON 역직렬화 및 DB 백업 조회)
     */
    public Account getAccount(Long accountId) {
        String key = OBJ_KEY + accountId;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account != null) {
                saveAccount(account, account.getMember());
            }
            return account;
        }

        try {
            return objectMapper.readValue(json, Account.class);
        } catch (Exception e) {
            log.error("역직렬화 실패 ID: {}", accountId, e);
            return null;
        }
    }

    /**
     * 6. 통합 저장: 객체, 번호, ExternalId, 회원 관계를 한 번에 갱신
     */
    public void saveAccount(Account account, Member member) {
        try {
            String accId = String.valueOf(account.getId());
            String memId = String.valueOf(member.getId());
            String json = objectMapper.writeValueAsString(account);

            // 데이터 정합성을 위해 5가지 매핑 정보를 모두 업데이트합니다 
            redisTemplate.opsForValue().set(OBJ_KEY + accId, json);
            redisTemplate.opsForValue().set(NUM_KEY + account.getAccountNumber(), accId);
            redisTemplate.opsForValue().set(EXT_KEY + account.getExternalId(), accId);
            redisTemplate.opsForValue().set(MEM_EXT_KEY + member.getExternalId(), memId);
            redisTemplate.opsForSet().add(MEM_ACCS_KEY + memId, accId);

            log.info("[Redis Sync] 계좌 캐시 동기화 완료: {}", account.getAccountNumber());
        } catch (Exception e) {
            log.error("직렬화 실패 계좌번호: {}", account.getAccountNumber(), e);
        }
    }

    /** * 7. 잔액 업데이트 및 비동기 동기화 
     */
    @Transactional
    public Long updateBalance(Long accountId, Long amount, boolean isIncrease) {
        Account account = getAccount(accountId);
        if (account == null) throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);

        if (isIncrease) {
            account.deposit(amount);
        } else {
            account.withdraw(amount);
        }

        // Redis 상태 즉시 업데이트 후 DB 동기화 이벤트 발행 
        saveAccount(account, account.getMember());
        eventPublisher.publishEvent(new BalanceSyncEvent(account.getId(), account.getBalance()));

        return account.getBalance();
    }
}