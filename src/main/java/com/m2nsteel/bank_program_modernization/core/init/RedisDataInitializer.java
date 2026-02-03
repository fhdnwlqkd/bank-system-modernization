package com.m2nsteel.bank_program_modernization.core.init;

import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class RedisDataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String ACCOUNT_BALANCE_KEY = "acc:bal:";

    @Override
    public void run(String... args) throws Exception {
        log.info("[Redis Warm-up] DB 잔액 데이터를 레디스로 복사하기 시작합니다...");

        // DB에서 모든 계좌 정보 조회
        List<Account> accounts = accountRepository.findAll();

        for (Account account : accounts) {
            String key = ACCOUNT_BALANCE_KEY + account.getId();
            String balance = String.valueOf(account.getBalance());

            // 2. 레디스에 잔액 저장
            // 이미 데이터가 있으면 덮어쓰고, 없으면 새로 생성
            redisTemplate.opsForValue().set(key, balance);
        }

        log.info("[Redis Warm-up] 총 {}개의 계좌 잔액 데이터가 레디스에 로드되었습니다!", accounts.size());
    }
}
