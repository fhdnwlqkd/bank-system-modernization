package com.m2nsteel.bank_program_modernization.controller;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.BranchStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.LoginRequest;
import com.m2nsteel.bank_program_modernization.dto.request.TransferRequest;
import com.m2nsteel.bank_program_modernization.dto.request.WithdrawRequest;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.BranchRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TransactionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BranchRepository branchRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String member1Token;
    private final String COMMON_PASSWORD = "password1234";
    private final String ACCOUNT_PASSWORD = "1234";

    @BeforeEach
    void setUp() throws Exception {
        // 1. 기본 지점 생성
        Branch branch = branchRepository.save(Branch.builder()
                .name("테스트 지점")
                .address("서울시 강남구")
                .branchCode("TEST-BRANCH-001")
                .createdAt(LocalDateTime.now())
                .status(BranchStatus.OPEN)
                .contact("010-1234-5678")
                .build());

        // 2. 기본 회원 생성
        Member member1 = memberRepository.save(Member.builder()
                .memberNumber("MEMBER-001")
                .loginId("member1")
                .password(passwordEncoder.encode(COMMON_PASSWORD))
                .name("회원1")
                .branchId(branch.getId())
                .role(MemberRole.USER)
                .contact("010-1111-2222")
                .status(MemberStatus.ACTIVE)
                .build());

        memberRepository.save(Member.builder()
                .memberNumber("MEMBER-002")
                .loginId("member2")
                .password(passwordEncoder.encode(COMMON_PASSWORD))
                .name("회원2")
                .branchId(branch.getId())
                .role(MemberRole.USER)
                .contact("010-1111-3333")
                .status(MemberStatus.ACTIVE)
                .build());

        // 3. 기본 계좌 생성
        accountRepository.save(Account.builder()
                .accountNumber("ACCOUNT-001")
                .memberId(member1.getId())
                .branchId(branch.getId())
                .balance(10000L)
                .status(AccountStatus.ACTIVE)
                .accountPassword(passwordEncoder.encode(ACCOUNT_PASSWORD))
                .createdAt(LocalDateTime.now())
                .build());

        accountRepository.save(Account.builder()
                .accountNumber("ACCOUNT-002")
                .memberId(2L)
                .branchId(branch.getId())
                .balance(5000L)
                .status(AccountStatus.ACTIVE)
                .accountPassword(passwordEncoder.encode(ACCOUNT_PASSWORD))
                .createdAt(LocalDateTime.now())
                .build());

        // 4. 테스트 시작 전 member1로 로그인하여 공통 토큰 획득
        this.member1Token = obtainAccessToken("member1", COMMON_PASSWORD);
    }

    /**
     * 로그인 API를 호출하여 JWT 토큰을 반환받는 헬퍼 메서드
     */
    private String obtainAccessToken(String loginId, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(loginId, password);

        String response = mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    @Test
    @DisplayName("입금 테스트: 잔액 증가 확인")
    void depositTest() throws Exception {
        // Given
        DepositRequest request = new DepositRequest(
                UUID.randomUUID().toString(), "ACCOUNT-001", 5000L
        );

        // When & Then
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(15000));
    }

    @Test
    @DisplayName("출금 테스트: 잔액 감소 확인")
    void withdrawTest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
                UUID.randomUUID().toString(), "ACCOUNT-001", 3000L, ACCOUNT_PASSWORD
        );

        // When & Then
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(7000));
    }

    @Test
    @DisplayName("이체 테스트: 송금인 잔액 감소 및 수취인 잔액 증가 확인")
    void transferTest() throws Exception {
        // Given
        TransferRequest request = new TransferRequest(
                UUID.randomUUID().toString(), "ACCOUNT-001", "ACCOUNT-002", ACCOUNT_PASSWORD, 5000L
        );

        // When
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(5000));

        // Then: 받는 사람 계좌(ACCOUNT-002)의 잔액이 실제로 늘어났는지 DB 검증
        Account targetAccount = accountRepository.findByAccountNumber("ACCOUNT-002")
                .orElseThrow();
        assertThat(targetAccount.getBalance()).isEqualTo(10000L); // 5000(초기) + 5000(이체금)
    }
}