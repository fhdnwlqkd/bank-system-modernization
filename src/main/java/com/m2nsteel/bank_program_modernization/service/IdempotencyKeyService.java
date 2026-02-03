package com.m2nsteel.bank_program_modernization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyKeyService {
    private final StringRedisTemplate redisTemplate;

    /**
     * 중복 요청인지 확인합니다.
     * @return true: 이미 존재함(중복), false: 처음 보는 키(성공적으로 저장됨)
     */
    public boolean isDuplicate(String key) {
        String redisKey = "idempotency:" + key;

        // setIfAbsent: 키가 없으면 저장하고 true 반환, 있으면 아무것도 안 하고 false 반환
        // 유효 기간은 24시간으로 설정
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "Y", Duration.ofHours(24));

        // success가 false라면 이미 키가 존재한다.
        return !Boolean.TRUE.equals(success);
    }
}
