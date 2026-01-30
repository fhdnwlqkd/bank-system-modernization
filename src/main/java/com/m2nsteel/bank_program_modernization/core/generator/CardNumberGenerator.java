package com.m2nsteel.bank_program_modernization.core.generator;

import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CardNumberGenerator {
    private final CardRepository cardRepository;
    public String generate() {
        // 1. 시퀀스 번호 획득
        Long seq = cardRepository.getNextCardSequence();

        // TODO: 카드 타입에 따른 구분 코드 설정
//        int typeDigit = switch (cardType) {
//            case CHECK -> 1;
//            case CREDIT -> 2;
//            default -> 0; // 예외 케이스
//        };
        int typeDigit = 1; // 지금은 체크카드만 발급한다고 가정

        // 3. 16자리 포맷팅
        return String.format("9410%d%tY%07d", typeDigit, LocalDateTime.now(), seq);
    }
}
