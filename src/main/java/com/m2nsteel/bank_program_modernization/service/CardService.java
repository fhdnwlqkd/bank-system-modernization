package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.*;
import com.m2nsteel.bank_program_modernization.domain.constant.CardStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
import com.m2nsteel.bank_program_modernization.repository.PaymentRepository;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
import com.m2nsteel.bank_program_modernization.service.mapper.CardMapper;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final CardMapper cardMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 카드 신규 발급
     */
    @Transactional
    public CardUsecase.CardResult issueCard(CardUsecase.IssueCardCommand command, String loginId) {
        // 1. 계좌 존재 여부 및 소유권 확인
        Account account = accountRepository.findByAccountNumber(command.accountNumber())
                .filter(a -> a.getMember().getLoginId().equals(loginId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 2. 카드 번호 및 유효기간 생성
        // 지금은 체크카드만 발급한다고 가정
        String cardNumber = cardNumberGenerator(CardType.CHECK);
        LocalDate expiryDate = LocalDate.now().plusYears(5); // 보통 5년

        // 3. 엔티티 생성 (정적 팩토리 메서드)
        Card card = Card.create(
                account,
                cardNumber,
                CardType.valueOf(command.cardType()),
                expiryDate
        );

        return cardMapper.toResult(cardRepository.save(card));
    }

    /**
     * 카드 상태 변경 (분실 신고, 해지 등)
     */
    @Transactional
    public CardUsecase.CardResult updateStatus(CardUsecase.UpdateCardStatusCommand command) {
        Card card = cardRepository.findByExternalId(command.externalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        card.changeStatus(CardStatus.valueOf(command.status()));

        return cardMapper.toResult(card);
    }

    @Transactional
    public CardUsecase.CardPaymentResult pay(CardUsecase.CardPaymentCommand command) {
        // 1. 검증 (카드, 비밀번호, 계좌)
        Card card = findActiveCard(command.cardNumber());
        verifyCardPassword(command.cardPassword(), card.getPassword());

        Account userAccount = card.getAccount();
        Account merchantAccount = findMerchantAccount(command.businessRegistrationNumber());

        if (userAccount.getId().equals(merchantAccount.getId())) {
            throw new BusinessException(ErrorCode.SELF_PAYMENT_NOT_ALLOWED);
        }

        // 2. 실질적 결제 처리 (잔액 이동)
        userAccount.withdraw(command.amount());
        merchantAccount.deposit(command.amount());

        // 3. Transaction(원장) 생성 및 기록
        Transaction transaction = Transaction.createPayment(command.amount(), command.idempotencyKey());
        TransactionItem.createWithdrawalItem(transaction, userAccount, command.amount(), 1);
        TransactionItem.createDepositItem(transaction, merchantAccount, command.amount(), 2);
        transactionRepository.save(transaction);

        // 4. Payment
        Payment payment = Payment.create(
                card,
                merchantAccount,
                transaction,
                command.amount(),
                command.idempotencyKey()
        );
        paymentRepository.save(payment);

        return cardMapper.toPaymentResult(payment, transaction, card, userAccount.getBalance());
    }

    @Transactional
    public CardUsecase.RefundResult refund(CardUsecase.RefundCommand command) {
        // 1. 원본 결제 건 조회
        Payment payment = paymentRepository.findByExternalId(command.paymentExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 환불 가능 금액 검증 및 누적 환불 금액 업데이트 (엔티티 내부 로직)
        payment.refund(command.amount());

        // 3. 자금 이동 (가맹점 계좌 -> 사용자 계좌)
        Account merchantAccount = payment.getMerchantAccount();
        Account userAccount = payment.getCard().getAccount();

        merchantAccount.withdraw(command.amount()); // 가맹점에서 차감
        userAccount.deposit(command.amount());     // 사용자에게 입금

        // 4. 환불용 Transaction 및 Item 기록
        Transaction refundTransaction = Transaction.createRefund(command.amount(), command.idempotencyKey());

        // 아이템 생성 (가맹점 출금, 사용자 입금)
        TransactionItem.createWithdrawalItem(refundTransaction, merchantAccount, command.amount(), 1);
        TransactionItem.createDepositItem(refundTransaction, userAccount, command.amount(), 2);

        transactionRepository.save(refundTransaction);

        // 5. 최종 결과 반환
        return cardMapper.toRefundResult(payment, refundTransaction);
    }

    // --- Private Helper Methods ---

    private Card findActiveCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CARD_NOT_ACTIVE);
        }
        return card;
    }

    private Account findMerchantAccount(String brn) {
        return accountRepository.findByBusinessNumber(brn)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ACCOUNT_NOT_FOUND));
    }

    private void verifyCardPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_CARD_PASSWORD);
        }
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
