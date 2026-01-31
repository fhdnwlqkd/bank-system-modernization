package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;
import com.m2nsteel.bank_program_modernization.repository.PaymentRepository;
import com.m2nsteel.bank_program_modernization.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MerchantSalesServiceTest {

    @Autowired private MerchantSalesService merchantSalesService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private CardService cardService;
    @Autowired private TransactionService transactionService;
    @Autowired private PaymentRepository paymentRepository;

    private String merchantOwnerId; // 가맹점주의 External ID
    private String merchantBizNo = "111-22-33333";
    private String userId;
    private String userCardId;

    @BeforeEach
    void setUp() {
        // 1. 가맹점주(Merchant Owner) 생성
        var owner = memberService.merchantSignUp(new MemberUsecase.MerchantSignUpCommand(
                "owner1", "pass123!", "1234", "가맹점주", "010-1111-2222",
                merchantBizNo, "민철네카페", "음식점"
        ));
        merchantOwnerId = owner.externalId();

        // 2. 결제 유저(User) 생성 및 계좌 입금
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "user1", "pass123!", "서민철", "010-3333-4444"));
        userId = user.externalId();

        var userAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(userId, "pass123!"));
        transactionService.deposit(new TransactionUsecase.DepositCommand(userAcc.accountNumber(), 100000L, "init"), userId);

        // 3. 카드 발급
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(
                userAcc.accountNumber(), "pass123!", "1234", "CHECK"), userId);
        userCardId = card.externalId();

        // 4. 결제 시나리오 생성 (T1 -> T3 순서대로 발생)
        // [P1] 결제: 10,000 (성공)
        cardService.pay(new CardUsecase.CardPaymentCommand(userCardId, 10000L, "1234", merchantBizNo, "pay-1"), userId);

        // [P2] 결제: 5,000 (성공)
        cardService.pay(new CardUsecase.CardPaymentCommand(userCardId, 5000L, "1234", merchantBizNo, "pay-2"), userId);

        // [P3] 결제 후 환불: 15,000 결제 -> 환불
        var pay3 = cardService.pay(new CardUsecase.CardPaymentCommand(userCardId, 15000L, "1234", merchantBizNo, "pay-3"), userId);
        cardService.refund(new CardUsecase.RefundCommand(pay3.paymentExternalId(), 15000L, "단순 변심", "ref-3"));
    }

    @Test
    @DisplayName("성공: 가맹점 매출 요약 집계 검증 (성공한 건만 합산)")
    void getSettlementSummary_Success() {
        // when
        var summary = merchantSalesService.getSettlementSummary(merchantOwnerId, LocalDate.now(), LocalDate.now());

        // then: 환불된 P3(15,000)를 제외하고 P1(10,000) + P2(5,000) = 15,000원 확인
        assertThat(summary.netAmount()).isEqualTo(15000L);
        assertThat(summary.totalTransactionCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("성공: 가맹점 매출 요약 집계 검증 (부분 환불 포함)")
    void getSettlementSummary_Success_PartialRefund() {
        // 추가 시나리오: 부분 환불 건 생성
        // [P4] 결제 후 부분 환불: 20,000 결제 -> 5,000 환불
        var pay4 = cardService.pay(new CardUsecase.CardPaymentCommand(userCardId, 20000L, "1234", merchantBizNo, "pay-4"), userId);
        cardService.refund(new CardUsecase.RefundCommand(pay4.paymentExternalId(), 5000L, "부분 환불", "ref-4"));

        // when
        var summary = merchantSalesService.getSettlementSummary(merchantOwnerId, LocalDate.now(), LocalDate.now());

        // then: P1(10,000) + P2(5,000) + P4(20,000 - 5,000) = 30,000원 확인
        assertThat(summary.netAmount()).isEqualTo(30000L);
        assertThat(summary.totalTransactionCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("성공: 가맹점 결제 내역 커서 기반 페이징 및 정렬 검증")
    void getPayments_CursorPaging_Success() {
        // given: size를 1로 설정하여 연속 조회
        var condition1 = new MerchantUsecase.MerchantPaymentSearchCondition(
                merchantOwnerId, null, null, null, null, 1);

        // when: 1페이지 조회 (가장 최신인 P3 환불 건이 나와야 함)
        var page1 = merchantSalesService.getPayments(merchantOwnerId, condition1);

        // then
        assertThat(page1.values()).hasSize(1);
        assertThat(page1.hasNext()).isTrue();
        Long cursor = page1.nextCursor();

        // when: 2페이지 조회 (P2 성공 건)
        var condition2 = new MerchantUsecase.MerchantPaymentSearchCondition(
                merchantOwnerId, null, null, null, cursor, 1);
        var page2 = merchantSalesService.getPayments(merchantOwnerId, condition2);

        // then
        assertThat(page2.values()).hasSize(1);
        assertThat(page2.values().getFirst().amount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 가맹점주 ID로 조회 시 에러 발생")
    void getPayments_InvalidMember_Fail() {
        var condition = new MerchantUsecase.MerchantPaymentSearchCondition(
                "wrong-id", null, null, null, null, 10);

        assertThatThrownBy(() -> merchantSalesService.getPayments("wrong-id", condition))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("성공: 특정 상태(REFUNDED) 필터링 조회")
    void getPayments_StatusFilter_Success() {
        // given: 환불된 건만 조회
        var condition = new MerchantUsecase.MerchantPaymentSearchCondition(
                merchantOwnerId, PaymentStatus.REFUNDED, null, null, null, 10);

        // when
        var result = merchantSalesService.getPayments(merchantOwnerId, condition);

        // then
        assertThat(result.values()).hasSize(1);
        assertThat(result.values().getFirst().amount()).isEqualTo(15000L);
        assertThat(result.values().getFirst().status()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
