package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Card;
import com.m2nsteel.bank_program_modernization.domain.constant.CardStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.dto.request.CardCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.response.CardResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {
    @Autowired private final CardRepository cardRepository;
    @Autowired private final AccountRepository accountRepository;
    @Autowired private final PasswordEncoder passwordEncoder;

    /*
    카드 생성
     */
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        // 1. 계좌 존재 여부 확인 TODO: 본인 계좌인지 확인 로직 추가 필요
        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 카드 번호 생성
        String cardNumber = cardNumberGenerator(CardType.valueOf(request.cardType()));
        LocalDateTime now = LocalDateTime.now();
        Card card = Card.builder()
                .cardNum(cardNumber)
                .accountId(account.getId())
                .password(passwordEncoder.encode(request.cardPassword()))
                .cardType(CardType.valueOf(request.cardType()))
                .status(CardStatus.ACTIVE)
                .issuedAt(now)
                .expiredAt(now.plusYears(8))
                .build();

        Card savedCard = cardRepository.save(card);

        // 3. 응답 DTO 변환
        String maskedCardNumber = cardNumber.replaceAll("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-****-****-$4");
        return new CardResponse(
                savedCard.getId(),
                maskedCardNumber,
                account.getAccountNumber(),
                savedCard.getCardType().name(),
                savedCard.getStatus().name(),
                savedCard.getExpiredAt()
        );
    }

    private String cardNumberGenerator(CardType cardType) {
        // 1. 시퀀스 번호 획득
        Long seq = cardRepository.getNextCardSequence();

        // 2. 카드 타입에 따른 구분 코드 설정
        int typeDigit = switch (cardType) {
            case CHECK -> 1;
            case CREDIT -> 2;
            default -> 0; // 예외 케이스
        };

        // 3. 16자리 포맷팅
        return String.format("9410%d%tY%07d", typeDigit, LocalDateTime.now(), seq);
    }
}
