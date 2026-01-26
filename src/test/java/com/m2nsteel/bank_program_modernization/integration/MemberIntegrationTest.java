package com.m2nsteel.bank_program_modernization.integration;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.constant.BranchStatus;
import com.m2nsteel.bank_program_modernization.dto.request.LoginRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MerchantSignUpRequest;
import com.m2nsteel.bank_program_modernization.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BranchRepository branchRepository;
    private final String branchCode = "TEST-BRANCH-001";
    @BeforeEach
    void setUp() throws Exception {
        // 기본 지점 생성
        Branch branch = Branch.builder()
                .name("테스트 지점")
                .address("서울시 강남구")
                .branchCode(branchCode)
                .createdAt(LocalDateTime.now())
                .status(BranchStatus.OPEN)
                .contact("010-1234-5678")
                .build();
        branchRepository.save(branch);
    }

    @Test
    @DisplayName("회원가입 -> 로그인 통합 테스트")
    void signUpAndLoginFlow() throws Exception {
        // 1. 회원가입 요청
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "realUser123",
                "password123!",
                "홍길동",
                branchCode
        );

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("realUser123"));

        // 2. 로그인 요청
        LoginRequest loginRequest = new LoginRequest("realUser123", "password123!");

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("가맹점 회원가입 -> 로그인 통합 테스트")
    void merchantSignUpAndLoginFlow() throws Exception {
        // 1. 회원가입 요청
        MerchantSignUpRequest merchantSignUpRequest = new MerchantSignUpRequest(
                "merchantUser123",
                "merchantPass123!",
                "가맹점주",
                branchCode,
                "1122334455",
                "가맹점 카테고리"
        );

        mockMvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantSignUpRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("merchantUser123"));

        // 2. 로그인 요청
        LoginRequest loginRequest = new LoginRequest("merchantUser123", "merchantPass123!");

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("JWT 토큰 발급 및 유효성 검증 테스트")
    void jwtTokenValidationFlow() throws Exception {
        // 1. 준비: 회원가입
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "authTester", "password123!", "인증테스터", branchCode
        );
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk());

        // 2. 로그인 및 토큰 추출
        LoginRequest loginRequest = new LoginRequest("authTester", "password123!");

        String loginResponse = mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String accessToken = JsonPath.read(loginResponse, "$.accessToken");

        // 3. 검증: 유효한 토큰으로 권한이 필요한 API 호출
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("authTester"));

        // 4. 검증: 토큰 없이 호출할 경우
        mockMvc.perform(get("/api/members/me"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
