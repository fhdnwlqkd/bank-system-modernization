package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.core.generator.CardNumberGenerator;
import com.m2nsteel.bank_program_modernization.domain.*;
import com.m2nsteel.bank_program_modernization.domain.constant.*;
import com.m2nsteel.bank_program_modernization.repository.*;
import com.m2nsteel.bank_program_modernization.repository.merchant.MerchantRepository;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionRepository;
import com.m2nsteel.bank_program_modernization.service.mapper.CardMapper;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CardMapper cardMapper;
    private final PasswordEncoder passwordEncoder;
    private final CardNumberGenerator generator;

    /**
     * 내 모든 카드 목록 조회
     */
    public List<CardUsecase.CardResult> getMyCards(String memberExternalId) {
        // 1. 회원의 externalId로 연결된 모든 카드 조회
        List<Card> cards = cardRepository.findAllByMemberExternalId(memberExternalId);

        // 2. 결과 매핑 후 반환
        return cards.stream()
                .map(cardMapper::toResult)
                .toList();
    }

    /**
     * 특정 카드 상세 조회
     */
    public CardUsecase.CardResult getCardDetail(String cardExternalId, String memberExternalId) {
        // 1. 카드 존재 여부 및 소유권 확인
        Card card = findActiveCardWithOwnership(cardExternalId, memberExternalId);

        return cardMapper.toResult(card);
    }

    /**
     * 결제 내역 조회
     */
    public List<CardUsecase.PaymentSummary> getPayments(String memberExternalId) {
        // 1. 카드 결제 내역 조회
        List<Payment> payments = paymentRepository.findAllByMemberExternalId(memberExternalId);

        // 2. 결과 매핑 후 반환
        return payments.stream()
                .map(payment -> {
                    Card card = payment.getCard();
                    String merchantName = payment.getMerchant().getMerchantName();
                    return cardMapper.toPaymentSummary(payment, card, merchantName);
                })
                .toList();

    }

    /**
     * 카드 신규 발급
     */
    @Transactional
    public CardUsecase.CardResult issueCard(CardUsecase.IssueCardCommand command, String memberExternalId) {
        // 1. 계좌 존재 여부 및 소유권 확인
        Account account = findActiveAccountWithOwnership(command.accountNumber(), memberExternalId);
        verifyAccountPassword(command.accountPassword(), account.getAccountPassword());

        // 2. 카드 번호 및 유효기간 생성
        String cardNumber = generator.generate();
        LocalDate expiredAt = LocalDate.now().plusYears(5); // 보통 5년

        // 3. 엔티티 생성
        Card card = Card.create(
                account,
                cardNumber,
                passwordEncoder.encode(command.cardPassword()),
                CardType.valueOf(command.cardType()),
                expiredAt
        );

        return cardMapper.toResult(cardRepository.save(card));
    }

    /**
     * 카드 상태 변경 (분실 신고, 해지 등)
     */
    @Transactional
    public CardUsecase.CardResult updateStatus(CardUsecase.UpdateCardStatusCommand command, String memberExternalId) {
        // 1. 카드 존재 여부 및 소유권 확인
        Card card = findActiveCardWithOwnership(command.cardExternalId(), memberExternalId);

        card.changeStatus(CardStatus.valueOf(command.status()));

        return cardMapper.toResult(card);
    }

    /**
     * 카드 비밀번호 변경
     */
    @Transactional
    public CardUsecase.CardResult changePassword(CardUsecase.ChangeCardPasswordCommand command, String cardExternalId, String memberExternalId) {
        // 1. 카드 존재 여부 및 소유권 확인
        Card card = findActiveCardWithOwnership(cardExternalId, memberExternalId);

        // 2. 현재 비밀번호 검증
        verifyCardPassword(command.oldPassword(), card.getPassword());

        // 3. 비밀번호 변경
        card.changePassword(passwordEncoder.encode(command.newPassword()));

        return cardMapper.toResult(card);
    }

    /**
     * 카드 결제 처리
     */
    @Transactional
    public CardUsecase.CardPaymentResult pay(CardUsecase.CardPaymentCommand command, String memberExternalId) {
        // 1. 멱등성 검증
        if(paymentRepository.existsByIdempotencyKey(command.idempotencyKey())) {
            throw new BusinessException(ErrorCode.REPEATED_REQUEST);
        }

        // 2. 결제 로직 수행 (기존 로직)
        Card card = findActiveCardWithOwnership(command.cardExternalId(), memberExternalId);
        verifyCardPassword(command.password(), card.getPassword());

        Account userAccount = card.getAccount();
        Merchant merchant = findMerchantByBusinessNumber(command.businessNumber());
        Account merchantAccount = findMerchantAccount(merchant);

        if (userAccount.getId().equals(merchantAccount.getId())) {
            throw new BusinessException(ErrorCode.SELF_PAYMENT_NOT_ALLOWED);
        }

        // 3. 실질적 자금 이동
        userAccount.withdraw(command.amount());
        merchantAccount.deposit(command.amount());

        // 4. 원장 및 비즈니스 기록 저장
        Transaction transaction = Transaction.createPayment(command.amount(), command.idempotencyKey());
        TransactionItem.createWithdrawalItem(transaction, userAccount, command.amount(), 1);
        TransactionItem.createDepositItem(transaction, merchantAccount, command.amount(), 2);
        transactionRepository.save(transaction);

        Payment payment = Payment.create(card, userAccount, merchant, merchantAccount, transaction, command.amount(), command.idempotencyKey());
        paymentRepository.save(payment);

        // 5. 신규 처리이므로 isRepeated = false
        return cardMapper.toPaymentResult(payment, transaction, card, userAccount, merchant.getMerchantName());
    }

    /**
     * 카드 결제 환불 처리
     */
    @Transactional
    public CardUsecase.RefundResult refund(CardUsecase.RefundCommand command) {
        // 1. 멱등성 검증
        if(refundRepository.existsByIdempotencyKey(command.idempotencyKey())) {
            throw new BusinessException(ErrorCode.REPEATED_REQUEST);
        }
        // 2. 신규 환불 로직 수행
        Payment payment = paymentRepository.findByExternalId(command.paymentExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 3. 엔티티 내부에서 환불 가능 금액 검증
        payment.refund(command.amount());

        // 4. 실질적 자금 이동
        Account merchantAccount = payment.getMerchantAccount();
        Account userAccount = payment.getCard().getAccount();

        merchantAccount.withdraw(command.amount());
        userAccount.deposit(command.amount());

        Transaction refundTransaction = Transaction.createRefund(command.amount(), command.idempotencyKey());
        TransactionItem.createWithdrawalItem(refundTransaction, merchantAccount, command.amount(), 1);
        TransactionItem.createDepositItem(refundTransaction, userAccount, command.amount(), 2);
        transactionRepository.save(refundTransaction);

        Refund refund = Refund.create(payment, command.amount(), command.reason(), refundTransaction, command.idempotencyKey());
        refundRepository.save(refund);

        return cardMapper.toRefundResult(payment, refund, refundTransaction);
    }

    // --- Helper Methods ---
    private Card findActiveCardWithOwnership(String cardExternalId, String externalId) {
        Card card = findActiveCard(cardExternalId);
        if (!card.getAccount().getMember().getExternalId().equals(externalId)) {
            throw new BusinessException(ErrorCode.NOT_CARD_OWNER);
        }
        return card;
    }

    private Card findActiveCard(String cardExternalId) {
        Card card = cardRepository.findByExternalId(cardExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CARD_NOT_ACTIVE);
        }
        return card;
    }

    private Merchant findMerchantByBusinessNumber(String brn) {
        return merchantRepository.findByBusinessNumber(brn)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND));
    }

    private Account findMerchantAccount(Member merchant) {
        return accountRepository.findByMember(merchant)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private void verifyCardPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_CARD_PASSWORD);
        }
    }

    private Account findActiveAccountWithOwnership(String accountNumber, String externalId) {
        Account account = findActiveAccount(accountNumber);
        if (!account.getMember().getExternalId().equals(externalId)) {
            throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
        }
        return account;
    }

    private Account findActiveAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private void verifyAccountPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
