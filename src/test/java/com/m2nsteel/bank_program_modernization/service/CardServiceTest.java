package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
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
class CardServiceTest {

    @Autowired private CardService cardService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;

    @Autowired private CardRepository cardRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundRepository refundRepository;

    private String memberId;
    private String merchantId;
    private String userAccountNo;
    private String merchantAccountNo;
    private final String PASSWORD = "password123!";
    private final String CARD_PASS = "1234";

    @BeforeEach
    void setUp() {
        // 1. Given: 일반 사용자 및 가맹점 가입
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "user1", PASSWORD, "사용자", "010-1111-2222"));
        var command = new MemberUsecase.MerchantSignUpCommand(
                "merchant1", PASSWORD, "1234", "카페주인", "010-3333-4444",
                "123-45-67890", "맛있는카페", "음식점"
        );
        var merchant = memberService.merchantSignUp(command);

        // when
        memberId = user.externalId();
        merchantId = merchant.externalId();

        // 2. Given: 각각의 계좌 개설
        var userAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(
                memberId, PASSWORD));

        userAccountNo = userAcc.accountNumber();
        merchantAccountNo = merchant.accountNumber();

        // 3. Given: 사용자 계좌에 50,000원 입금 (결제 자금 확보)
        transactionService.deposit(new TransactionUsecase.DepositCommand(
                userAccountNo, 50000L, "idemp-setup-3"), memberId);
    }

    @Test
    @DisplayName("성공: 카드 발급")
    void issue_Card_Success() {
        var issueCmd = new CardUsecase.IssueCardCommand(userAccountNo, PASSWORD, CARD_PASS, "CHECK");

        var result = cardService.issueCard(issueCmd, memberId);

        assertThat(result.externalId()).isNotEmpty();
        assertThat(cardRepository.existsByExternalId(result.externalId())).isTrue();
    }

    @Test
    @DisplayName("성공: 카드 발급부터 결제 및 환불까지의 전체 흐름 검증")
    void full_Card_Lifecycle_Test() {
        // --- [Step 1: 카드 발급] ---
        var issueCmd = new CardUsecase.IssueCardCommand(userAccountNo, PASSWORD, CARD_PASS, "CHECK");
        var cardResult = cardService.issueCard(issueCmd, memberId);

        assertThat(cardResult.externalId()).isNotEmpty();
        assertThat(cardRepository.existsByExternalId(cardResult.externalId())).isTrue();

        // --- [Step 2: 카드 결제] ---
        String payKey = "pay-key-101";
        var payCmd = new CardUsecase.CardPaymentCommand(
                cardResult.externalId(), 10000L, CARD_PASS, "123-45-67890", payKey);

        var payResult = cardService.pay(payCmd, memberId);

        // Then: 결제 성공 확인
        assertThat(payResult.isRepeated()).isFalse();
        assertThat(payResult.amount()).isEqualTo(10000L);
        assertThat(accountRepository.findByAccountNumber(userAccountNo).get().getBalance()).isEqualTo(40000L);

        // Idempotency: 동일 키로 재결제 시도
        var payRetryResult = cardService.pay(payCmd, memberId);
        assertThat(payRetryResult.isRepeated()).isTrue();
        assertThat(accountRepository.findByAccountNumber(userAccountNo).get().getBalance()).isEqualTo(40000L); // 잔액 불변

        // --- [Step 3: 카드 환불 (부분 환불)] ---
        String refundKey = "refund-key-201";
        var refundCmd = new CardUsecase.RefundCommand(payResult.paymentExternalId(), 4000L, "단순 변심", refundKey);

        var refundResult = cardService.refund(refundCmd);

        // Then: 환불 성공 확인 (사용자 잔액 복구)
        assertThat(refundResult.isRepeated()).isFalse();
        assertThat(refundResult.refundAmount()).isEqualTo(4000L);
        assertThat(accountRepository.findByAccountNumber(userAccountNo).get().getBalance()).isEqualTo(44000L); // 40,000 + 4,000

        // Idempotency: 동일 키로 재환불 시도
        var refundRetryResult = cardService.refund(refundCmd);
        assertThat(refundRetryResult.isRepeated()).isTrue();
        assertThat(accountRepository.findByAccountNumber(userAccountNo).get().getBalance()).isEqualTo(44000L); // 잔액 불변
    }

    @Test
    @DisplayName("성공: 카드 상태 변경 (분실 신고) 후 결제 시도 시 실패")
    void card_Status_Update_And_Payment_Fail() {
        // 1. Given: 카드 발급
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(
                userAccountNo, PASSWORD, CARD_PASS, "CHECK"), memberId);

        // 2. When: 카드 분실 신고 (Service 사용)
        cardService.updateStatus(new CardUsecase.UpdateCardStatusCommand(
                card.externalId(), CARD_PASS, "LOST"), memberId);

        // 3. Then: 결제 시도 시 CARD_NOT_ACTIVE 예외 발생
        var payCmd = new CardUsecase.CardPaymentCommand(
                card.externalId(), 1000L, CARD_PASS, "123-45-67890", "idemp-lost-1");

        assertThatThrownBy(() -> cardService.pay(payCmd, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_ACTIVE);
    }
}
