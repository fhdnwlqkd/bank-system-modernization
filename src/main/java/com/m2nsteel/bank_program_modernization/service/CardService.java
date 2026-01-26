package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Card;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.CardStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.m2nsteel.bank_program_modernization.dto.request.CardCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.CardPaymentRequest;
import com.m2nsteel.bank_program_modernization.dto.response.CardCreateResponse;
import com.m2nsteel.bank_program_modernization.dto.response.CardPaymentResponse;
import com.m2nsteel.bank_program_modernization.dto.response.TransferResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
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
    @Autowired private final TransactionRepository transactionRepository;
    @Autowired private final PasswordEncoder passwordEncoder;

    /*
    카드 생성
     */
    @Transactional
    public CardCreateResponse createCard(CardCreateRequest request) {
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
        String masked = cardNumberMasking(savedCard.getCardNum());

        // 3. 응답 DTO 변환
        return new CardCreateResponse(
                savedCard.getId(),
                masked,
                account.getAccountNumber(),
                savedCard.getCardType().name(),
                savedCard.getStatus().name(),
                savedCard.getExpiredAt()
        );
    }

   /*
   카드 결제
    */
    @Transactional
    public CardPaymentResponse pay(CardPaymentRequest request) {
        // 1. Idempotency Key 중복 체크
        if (transactionRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 카드 조회
        Card card = cardRepository.findByCardNum(request.cardNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // 3. 카드 상태 확인
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CARD_NOT_ACTIVE);
        }

        // 4. 카드 비밀번호 확인
        if (!passwordEncoder.matches(request.cardPassword(), card.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 5. 카드 계좌 조회
        Account cardAccount = accountRepository.findById(card.getAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 6. 가맹점 계좌 존재 여부 확인
        Account merchantAccount = accountRepository.findByBRN(request.businessRegistrationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 7. 자기 계좌 결제 방지
        if (cardAccount.getId().equals(merchantAccount.getId())) {
            throw new BusinessException(ErrorCode.SELF_PAYMENT_NOT_ALLOWED);
        }

        // 8. 결제 처리 로직
        cardAccount.withdraw(request.amount());
        merchantAccount.deposit(request.amount());

        Transaction transaction = Transaction.builder()
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .requestId(request.requestId())
                .occurredAt(LocalDateTime.now())
                .build();

        TransactionItem withdrawItem = TransactionItem.builder()
                .transaction(transaction)
                .account(cardAccount)
                .delta(-request.amount())
                .balanceAfter(cardAccount.getBalance())
                .occurredAt(transaction.getOccurredAt())
                .itemOrder(1)
                .build();

        TransactionItem depositItem = TransactionItem.builder()
                .transaction(transaction)
                .account(merchantAccount)
                .delta(request.amount())
                .balanceAfter(merchantAccount.getBalance())
                .occurredAt(transaction.getOccurredAt())
                .itemOrder(2)
                .build();

        transaction.addItem(withdrawItem);
        transaction.addItem(depositItem);

        transactionRepository.save(transaction);

        // 9. 응답 DTO 반환
        String maskedCardNumber = cardNumberMasking(card.getCardNum());

        return new CardPaymentResponse(
                transaction.getId(),
                maskedCardNumber,
                transaction.getAmount(),
                withdrawItem.getBalanceAfter(),
                request.businessRegistrationNumber(),
                transaction.getStatus().name(),
                transaction.getOccurredAt()
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

    private String cardNumberMasking(String cardNumber) {
        String cleanNumber = cardNumber.replaceAll("\\D", "");
        return cleanNumber.replaceAll("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-****-****-$4");
    }
}
