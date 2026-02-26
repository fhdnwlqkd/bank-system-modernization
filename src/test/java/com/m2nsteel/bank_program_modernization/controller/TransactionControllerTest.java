package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.TransactionDto;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class TransactionControllerTest {

    @Autowired private MockMvcTester mvc;
    @Autowired private JsonMapper jsonmapper;

    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;

    private String accessToken;
    private String myAccountNumber;
    private String otherAccountNumber;

    private final String LOGIN_ID = "txTester";
    private final String PW = "Password123!";
    private final String ACC_PW = "1234";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 1. 내 계좌 셋업 (Service Layer) 
        var me = memberService.signUp(new MemberUsecase.MemberSignUpCommand(LOGIN_ID, PW, "나", "010-1111-1111"));
        var myAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(me.externalId(), ACC_PW));
        this.myAccountNumber = myAcc.accountNumber();

        // 2. 이체 대상 계좌 셋업 (Service Layer) 
        var other = memberService.signUp(new MemberUsecase.MemberSignUpCommand("otherUser", PW, "너", "010-2222-2222"));
        var otherAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(other.externalId(), "5678"));
        this.otherAccountNumber = otherAcc.accountNumber();

        // 3. 로그인 및 토큰 획득 
        var loginReq = new AuthDto.LoginRequest(LOGIN_ID, PW);
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .satisfies(token -> this.accessToken = token.toString());
    }

    @Test
    @DisplayName("성공: 입금 처리 검증")
    void deposit_Success() throws JsonProcessingException {
        var request = new TransactionDto.DepositRequest(myAccountNumber, 10000L, UUID.randomUUID().toString());

        assertThat(mvc.post().uri("/api/transactions/deposit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(TransactionDto.GeneralResponse.class)
                .satisfies(res -> {
                    assertThat(res.amount()).isEqualTo(10000L);
                    assertThat(res.type()).isEqualTo("DEPOSIT");
                });
    }

    @Test
    @DisplayName("성공: 출금 처리 검증")
    void withdraw_Success() throws JsonProcessingException {
        // 먼저 입금해서 잔액 만들기 
        deposit(20000L);

        var request = new TransactionDto.WithdrawRequest(myAccountNumber, 10000L, ACC_PW, UUID.randomUUID().toString());

        assertThat(mvc.post().uri("/api/transactions/withdraw")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(TransactionDto.GeneralResponse.class)
                .satisfies(res -> {
                    assertThat(res.amount()).isEqualTo(10000L);
                    assertThat(res.type()).isEqualTo("WITHDRAW");
                });
    }

    @Test
    @DisplayName("성공: 이체 처리 검증")
    void transfer_Success() throws JsonProcessingException {
        // 잔액 확보 
        deposit(50000L);

        var request = new TransactionDto.TransferRequest(myAccountNumber, otherAccountNumber, 30000L, ACC_PW, UUID.randomUUID().toString());

        assertThat(mvc.post().uri("/api/transactions/transfer")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(TransactionDto.TransferResponse.class)
                .satisfies(res -> {
                    assertThat(res.amount()).isEqualTo(30000L);
                    assertThat(res.fromAccountNumber()).isEqualTo(myAccountNumber);
                    assertThat(res.toAccountNumber()).isEqualTo(otherAccountNumber);
                });
    }

    @Test
    @DisplayName("실패: 동일한 Idempotency Key로 중복 요청 시 REPEATED_REQUEST 에러")
    void transaction_Fail_DuplicateRequest() throws JsonProcessingException {
        String key = UUID.randomUUID().toString();
        var request = new TransactionDto.DepositRequest(myAccountNumber, 1000L, key);

        // 첫 번째 요청 
        mvc.post().uri("/api/transactions/deposit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request))
                .exchange();

        // 두 번째 동일 요청 
        assertThat(mvc.post().uri("/api/transactions/deposit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> assertThat(res.errorName()).contains("REPEATED_REQUEST"));
    }

    @Test
    @DisplayName("실패: 잔액 부족 시 출금 실패")
    void withdraw_Fail_InsufficientBalance() throws JsonProcessingException {
        var request = new TransactionDto.WithdrawRequest(myAccountNumber, 9999999L, ACC_PW, UUID.randomUUID().toString());

        assertThat(mvc.post().uri("/api/transactions/withdraw")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> assertThat(res.errorName()).contains("INSUFFICIENT_BALANCE"));
    }

    // --- Helper ---
    private void deposit(Long amount) throws JsonProcessingException {
        var request = new TransactionDto.DepositRequest(myAccountNumber, amount, UUID.randomUUID().toString());
        mvc.post().uri("/api/transactions/deposit")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request))
                .exchange();
    }
}