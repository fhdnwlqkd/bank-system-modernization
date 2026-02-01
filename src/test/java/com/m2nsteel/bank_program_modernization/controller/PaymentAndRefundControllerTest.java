package com.m2nsteel.bank_program_modernization.controller;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.CardDto;
import com.m2nsteel.bank_program_modernization.service.*;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class PaymentAndRefundControllerTest {

    @Autowired private MockMvcTester mvc;
    @Autowired private JsonMapper jsonmapper;

    // 데이터 준비를 위한 서비스 주입
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private CardService cardService;
    @Autowired private TransactionService transactionService;

    private String accessToken;
    private String cardId;
    private String merchantBrn = "123-45-67890";

    private final String PASSWORD = "password123!";
    private final String CARD_PASS = "1234";

    @BeforeEach
    void setUp() throws Exception {
        // 1. 서비스 레이어를 통해 테스트 데이터 준비
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "user1", PASSWORD, "사용자", "010-1111-2222"));

        var merchantCommand = new MemberUsecase.MerchantSignUpCommand(
                "merchant1", PASSWORD, "1234", "카페주인", "010-3333-4444",
                merchantBrn, "맛있는카페", "음식점"
        );
        memberService.merchantSignUp(merchantCommand);

        var userAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(
                user.externalId(), PASSWORD));

        // 결제 자금 확보
        transactionService.deposit(new TransactionUsecase.DepositCommand(
                userAcc.accountNumber(), 50000L, "idemp-setup-" + UUID.randomUUID()), user.externalId());

        // 카드 발급
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(
                userAcc.accountNumber(), PASSWORD, CARD_PASS, "CHECK"), user.externalId());

        this.cardId = card.externalId();

        // 2. 컨트롤러 테스트를 위한 토큰 획득 (MockMvc 사용)
        this.accessToken = obtainAccessToken("user1", PASSWORD);
    }

    @Test
    @DisplayName("성공: 카드 결제 수행 및 잔액 확인")
    void pay_Success() throws Exception {
        var request = new CardDto.CardPaymentRequest(cardId, 10000L, CARD_PASS, merchantBrn, "pay-key-1");

        assertThat(mvc.post().uri("/api/cards/pay")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CardDto.CardPaymentResponse.class)
                .satisfies(res -> {
                    assertThat(res.amount()).isEqualTo(10000L);
                    assertThat(res.balanceAfter()).isEqualTo(40000L);
                    assertThat(res.merchantName()).isEqualTo("맛있는카페");
                });
    }

    @Test
    @DisplayName("성공: 결제 후 부분 환불 처리")
    void refund_Success() throws Exception {
        // 1. 먼저 결제 수행
        String payKey = "pay-key-2";
        var payRequest = new CardDto.CardPaymentRequest(cardId, 20000L, CARD_PASS, merchantBrn, payKey);
        var exchange = mvc.post().uri("/api/cards/pay")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(payRequest))
                .exchange();

        String body = new String(exchange.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        var root = jsonmapper.readTree(body);
        var dataNode = root.at("/data");

        var payResponse = jsonmapper.treeToValue(dataNode, CardDto.CardPaymentResponse.class);

        // 2. 환불 수행 (5,000원 부분 환불)
        var refundRequest = new CardDto.RefundRequest(
                payResponse.paymentExternalId(),
                5000L,
                "단순 변심",
                "ref-key-1"
        );

        assertThat(mvc.post().uri("/api/cards/refund")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(refundRequest)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CardDto.RefundResponse.class)
                .satisfies(res -> {
                    assertThat(res.refundAmount()).isEqualTo(5000L);
                    assertThat(res.remainingAmount()).isEqualTo(15000L);
                    assertThat(res.status()).isEqualTo("PARTIAL_REFUNDED");
                });
    }

    @Test
    @DisplayName("실패: 결제 시 멱등성 키 중복 요청 방지")
    void pay_Fail_RepeatedRequest() throws Exception {
        String key = "duplicate-key";
        var request = new CardDto.CardPaymentRequest(cardId, 1000L, CARD_PASS, merchantBrn, key);

        // 첫 번째 요청
        mvc.post().uri("/api/cards/pay")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request))
                .exchange();

        // 두 번째 동일 요청
        assertThat(mvc.post().uri("/api/cards/pay")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.error.code")
                .isEqualTo(ErrorCode.REPEATED_REQUEST.getCode());
    }

    @Test
    @DisplayName("실패: 타인 소유의 카드로 결제 시도 시 거절")
    void pay_Fail_NotOwner() throws Exception {
        // 다른 사용자 생성
        memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "other", PASSWORD, "타인", "010-0000-0000"));
        String otherToken = obtainAccessToken("other", PASSWORD);

        var request = new CardDto.CardPaymentRequest(cardId, 1000L, CARD_PASS, merchantBrn, "other-pay-key");

        assertThat(mvc.post().uri("/api/cards/pay")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.error.errorName")
                .asString().contains("NOT_CARD_OWNER");
    }

    /**
     * 로그인을 통해 토큰을 발급받는 헬퍼 메서드
     */
    private String obtainAccessToken(String loginId, String password) throws Exception {
        var loginRequest = new AuthDto.LoginRequest(loginId, password);

        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest))
                .exchange();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}