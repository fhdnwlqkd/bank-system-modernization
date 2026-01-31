package com.m2nsteel.bank_program_modernization.service.transaction;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.m2nsteel.bank_program_modernization.service.*;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test") // application-test.yml (H2 DB) 사용
@Transactional
class TransactionSearchServiceTest {

    @Autowired private TransactionSearchService searchService;
    @Autowired private TransactionService transactionService;
    @Autowired private CardService cardService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;

    private String userId;
    private String userAccountNo;
    private String userAccountExternalId;

    private String receiverAccountNo;
    private String merchantId;
    private String cardExternalId;

    private final String PASS = "password123!";
    private final String CARD_PASS = "1234";
    private final String MERCHANT_BIZ_NO = "123-45-67890";

    @BeforeEach
    void setUp() {
        // 1. 송금인(User) 및 수취인(Receiver) 가입
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "user1", PASS, "서민철", "010-1111-2222"));
        userId = user.externalId();

        var receiver = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "receiver1", PASS, "수취인", "010-4444-5555"));

        // 2. 가맹점(Merchant) 가입
        var merchant = memberService.merchantSignUp(new MemberUsecase.MerchantSignUpCommand(
                "merchant1", PASS, "1234", "맛있는카페", "010-3333-4444",
                MERCHANT_BIZ_NO, "맛있는카페", "음식점"
        ));
        merchantId = merchant.externalId();

        // 3. 각각의 계좌 개설
        var userAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(userId, PASS));
        userAccountNo = userAcc.accountNumber();
        userAccountExternalId = userAcc.externalId();

        var receiverAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(receiver.externalId(), PASS));
        receiverAccountNo = receiverAcc.accountNumber();

        // 4. 카드 발급
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(
                userAccountNo, PASS, CARD_PASS, "CHECK"), userId);
        cardExternalId = card.externalId();

        // 5. 복합 거래 시나리오 생성 (T1 -> T5 순서대로 발생, 조회 시 역순 노출)
        // [T1] 입금: +50,000 (잔액: 50,000)
        transactionService.deposit(new TransactionUsecase.DepositCommand(userAccountNo, 50000L, "tx-1"), userId);

        // [T2] 출금: -5,000 (잔액: 45,000)
        transactionService.withdraw(new TransactionUsecase.WithdrawCommand(userAccountNo, 5000L, PASS, "tx-2"), userId);

        // [T3] 가맹점 결제: -10,000 (잔액: 35,000)
        var pay = cardService.pay(new CardUsecase.CardPaymentCommand(
                cardExternalId, 10000L, CARD_PASS, MERCHANT_BIZ_NO, "tx-3"), userId);

        // [T4] 환불(부분): +4,000 (잔액: 39,000)
        cardService.refund(new CardUsecase.RefundCommand(pay.paymentExternalId(), 4000L, "단순 변심", "tx-4"));

        // [T5] 이체: -9,000 (잔액: 30,000)
        transactionService.transfer(new TransactionUsecase.TransferCommand(
                userAccountNo, receiverAccountNo, 9000L, PASS, "tx-5"), userId);
    }

    @Test
    @DisplayName("성공: 커서 기반 첫 페이지 조회 및 정렬/잔액 검증")
    void search_FirstPage_And_Balance_Success() {
        var condition = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, null, null, null, null);
        var pageable = PageRequest.of(0, 5);

        var result = searchService.searchTransactions(userId, false, condition, pageable);

        assertThat(result.values()).hasSize(5);
        // 최신순 정렬 확인 (T5 - TRANSFER)
        assertThat(result.values().get(0).type()).isEqualTo("TRANSFER");
        // 최종 잔액 흐름 확인 (T5 이후 30,000원)
        assertThat(result.values().get(0).balanceAfter()).isEqualTo(30000L);
        assertThat(result.hasNext()).isFalse(); // 전체가 5개이므로 다음 페이지 없음
    }

    @Test
    @DisplayName("성공: 커서 기반 연속 조회 (Next Cursor 페이징)")
    void search_Sequential_Cursor_Success() {
        // 1. 1페이지 (3개: T5, T4, T3)
        var cond1 = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, null, null, null, null);
        var page1 = searchService.searchTransactions(userId, false, cond1, PageRequest.of(0, 3));

        assertThat(page1.hasNext()).isTrue();
        Long cursor = page1.nextCursor();

        // 2. 2페이지 (남은 2개: T2, T1)
        var cond2 = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, null, null, null, cursor);
        var page2 = searchService.searchTransactions(userId, false, cond2, PageRequest.of(0, 3));

        assertThat(page2.values()).hasSize(2);
        assertThat(page2.values().get(0).type()).isEqualTo("WITHDRAW"); // T2
        assertThat(page2.values().get(1).type()).isEqualTo("DEPOSIT");  // T1
        assertThat(page2.hasNext()).isFalse();
    }

    @Test
    @DisplayName("성공: 관리자 권한으로 타인의 계좌 조회")
    void search_Admin_Bypass_Success() {
        var condition = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, null, null, null, null);

        // 관리자 ID(merchantId 등 아무나)로 isAdmin=true 설정하여 조회
        var result = searchService.searchTransactions(merchantId, true, condition, PageRequest.of(0, 10));
        System.out.println("result = " + result);
        assertThat(result.values()).hasSize(5);
    }

    @Test
    @DisplayName("실패: 소유주가 아닌 사용자의 접근 차단")
    void search_Unauthorized_Fail() {
        var condition = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, null, null, null, null);

        // 관리자가 아닌 타인이 조회 시도
        assertThatThrownBy(() -> searchService.searchTransactions(merchantId, false, condition, PageRequest.of(0, 5)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCOUNT_OWNER);
    }

    @Test
    @DisplayName("성공: 복합 필터링 (PAYMENT 유형 + 특정 금액대)")
    void search_Complex_Filter_Success() {
        // 결제(10,000원) 건을 찾기 위한 필터
        var condition = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, null, null, TransactionType.PAYMENT, 9000L, 11000L, null);

        var result = searchService.searchTransactions(userId, false, condition, PageRequest.of(0, 10));

        assertThat(result.values()).hasSize(1);
        assertThat(result.values().getFirst().type()).isEqualTo("PAYMENT");
        assertThat(result.values().getFirst().amount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("성공: 기간 검색 필터링")
    void search_Date_Filter_Success() {
        // 오늘 날짜로 범위 지정 (T1~T5 모두 오늘 발생)
        var condition = new TransactionUsecase.TransactionSearchCondition(
                userAccountExternalId, java.time.LocalDate.now(), java.time.LocalDate.now(), null, null, null, null);

        var result = searchService.searchTransactions(userId, false, condition, PageRequest.of(0, 10));

        assertThat(result.values()).hasSize(5);
    }
}