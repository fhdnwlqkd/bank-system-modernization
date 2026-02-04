package com.m2nsteel.bank_program_modernization.core.init;

import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.service.RedisAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class RedisDataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final RedisAccountService redisAccountService;

    @Override
    public void run(String... args) throws Exception {
        log.info("[Redis Warm-up] DB 데이터를 기반으로 Redis 캐시 웜업을 시작합니다...");

        // 1. DB에서 모든 계좌와 연관된 Member 정보까지 한 번에 가져옵니다 
        List<Account> accounts = accountRepository.findAll();

        if (accounts.isEmpty()) {
            log.info("[Redis Warm-up] 로드할 계좌 데이터가 없습니다. ");
            return;
        }

        // 2. 각 계좌를 순회하며 Redis에 3종 세트(객체, 번호매핑, 관계)를 저장합니다 
        for (Account account : accounts) {
            try {
                redisAccountService.saveAccount(account, account.getMember());
            } catch (Exception e) {
                log.error("[Redis Warm-up] 계좌 캐싱 실패 - ID: {}, Error: {}", account.getId(), e.getMessage());
            }
        }

        log.info("[Redis Warm-up] 총 {}개의 계좌 데이터 및 관계 인덱스 로드가 완료되었습니다! ", accounts.size());
    }
}