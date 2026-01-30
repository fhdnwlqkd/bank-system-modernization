package com.m2nsteel.bank_program_modernization.core.generator;

import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
    private final AccountRepository accountRepository;

    /**
     * 계좌번호 생성: 110-XXX-XXXXXXXXXX (총 18자, 숫자 16자리)
     */
    public String generate() {
        // 1. DB Sequence - 7자리 (0 ~ 9,999,999)
        long seq = accountRepository.getNextAccountSequence() % 10000000;

        // 2. 현재 시간의 밀리초 마지막 3자리 (0 ~ 999)
        long timePart = System.currentTimeMillis() % 1000;

        // 3. 난수 3자리 (0 ~ 999) - 예측 불가능성
        int randomPart = ThreadLocalRandom.current().nextInt(0, 1000);

        // 4. 모든 파라미터를 하나의 포맷으로 결합
        return String.format("110-%03d-%07d%03d", randomPart, seq, timePart);
    }
}
