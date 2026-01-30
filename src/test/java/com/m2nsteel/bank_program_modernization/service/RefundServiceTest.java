package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.PaymentRepository;
import com.m2nsteel.bank_program_modernization.repository.RefundRepository;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class RefundServiceTest {

    @Autowired private CardService cardService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;

    @Autowired private AccountRepository accountRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundRepository refundRepository;

    private String memberId;
    private String userAccNo;
    private String cardId;
    private String merchantBrn = "123-45-67890";
    private final String PASS = "password123!";
    private final String CARD_PASS = "1234";

    @BeforeEach
    void setUp() {
        // 1. Given: 사용자 및 계좌 준비
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand("mincheol", PASS, "서민철", "010-1234-5678"));
        memberId = user.externalId();
        var acc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberId, PASS));
        userAccNo = acc.accountNumber();

        // 2. Given: 초기 잔액 입금 및 카드 발급
        transactionService.deposit(new TransactionUsecase.DepositCommand(userAccNo, 100000L, "setup-deposit"), memberId);
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(userAccNo, PASS, CARD_PASS, "CHECK"), memberId);
        cardId = card.externalId();

        // 3. 가맹점은 사업자 번호가 포함된 특수 가입 혹은 설정을 따름
        var command = new MemberUsecase.MerchantSignUpCommand(
                "merchant1", PASS, "1234", "카페주인", "010-3333-4444",
                merchantBrn, "맛있는카페", "음식점"
        );
        var merchant = memberService.merchantSignUp(command);
    }

    @Test
    @DisplayName("성공: 부분 환불 시나리오 - 잔액 복구 및 누적 금액 검증")
    void partial_refund_success() {
        // 1. Given: 10,000원 결제 수행
        var payCmd = new CardUsecase.CardPaymentCommand(cardId, 10000L, CARD_PASS, merchantBrn, "pay-key-1");
        var payResult = cardService.pay(payCmd, memberId);

        // 2. When: 4,000원 부분 환불 요청
        var refundCmd = new CardUsecase.RefundCommand(payResult.paymentExternalId(), 4000L, "단순 변심", "refund-key-1");
        var result = cardService.refund(refundCmd);

        // 3. Then: 결과 확인
        assertThat(result.isRepeated()).isFalse();
        assertThat(result.refundAmount()).isEqualTo(4000L);
        assertThat(result.totalRefundedAmount()).isEqualTo(4000L);
        assertThat(result.remainingAmount()).isEqualTo(6000L); // 10,000 - 4,000

        // 4. Then: 실제 계좌 잔액 복구 확인 (90,000 + 4,000)
        Account userAccount = accountRepository.findByAccountNumber(userAccNo).orElseThrow();
        assertThat(userAccount.getBalance()).isEqualTo(94000L);
    }

    @Test
    @DisplayName("성공: 멱등성 검증 - 동일한 환불 키로 재요청 시 중복 환불 방지")
    void refund_idempotency_test() {
        // 1. Given: 결제 및 1차 환불 완료
        var payResult = cardService.pay(new CardUsecase.CardPaymentCommand(cardId, 10000L, CARD_PASS, merchantBrn, "pay-key-2"), memberId);
        var refundCmd = new CardUsecase.RefundCommand(payResult.paymentExternalId(), 5000L, "오주문", "refund-idemp-key");
        cardService.refund(refundCmd);

        // 2. When: 동일한 키로 2차 환불 요청
        var secondResult = cardService.refund(refundCmd);

        // 3. Then: 멱등성 결과 확인
        assertThat(secondResult.isRepeated()).isTrue();
        assertThat(secondResult.refundAmount()).isEqualTo(5000L);

        // 4. Then: 잔액이 두 번 복구되지 않았는지 확인 (90,000 + 5,000)
        Account userAccount = accountRepository.findByAccountNumber(userAccNo).orElseThrow();
        assertThat(userAccount.getBalance()).isEqualTo(95000L);
    }

    @Test
    @DisplayName("실패: 환불 가능 금액 초과 시 BusinessException 발생")
    void refund_exceed_amount_fail() {
        // 1. Given: 10,000원 결제
        var payResult = cardService.pay(new CardUsecase.CardPaymentCommand(cardId, 10000L, CARD_PASS, merchantBrn, "pay-key-3"), memberId);

        // 2. When & Then: 15,000원 환불 시도 시 에러
        var refundCmd = new CardUsecase.RefundCommand(payResult.paymentExternalId(), 15000L, "실수", "refund-fail-key");

        assertThatThrownBy(() -> cardService.refund(refundCmd))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEED_REFUND_AMOUNT);
    }
}
