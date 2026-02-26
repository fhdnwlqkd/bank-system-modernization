package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper jsonmapper;

    private String authenticatedExternalId; // 가입 시 획득한 식별자
    private String accessToken;            // 로그인 시 획득한 토큰

    private final String LOGIN_ID = "tester123";
    private final String PASSWORD = "Password123!";
    private final String NAME = "홍길동";
    private final String CONTACT = "010-1234-5678";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 1. 회원가입 실행 및 externalId 추출
        var signUpReq = new MemberDto.MemberSignUpRequest(LOGIN_ID, PASSWORD, NAME, CONTACT);
        assertThat(mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(signUpReq)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> this.authenticatedExternalId = id.toString());

        // 2. 실제 로그인 호출 및 accessToken 추출
        var loginReq = new AuthDto.LoginRequest(LOGIN_ID, PASSWORD);
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .satisfies(token -> this.accessToken = token.toString());
    }

    // --- 1. 내 정보 관리 테스트 ---

    @Test
    @DisplayName("성공: 실제 로그인 토큰을 사용하여 내 정보 상세 조회 검증")
    void getMyInfo_Success() {
        assertThat(mvc.get().uri("/api/members/me")
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(MemberDto.MemberResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo(LOGIN_ID);
                    assertThat(res.name()).isEqualTo(NAME);
                    assertThat(res.contact()).isEqualTo(CONTACT);
                    assertThat(res.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(res.externalId()).isEqualTo(authenticatedExternalId);
                });
    }

    @Test
    @DisplayName("성공: 내 정보 수정 시 변경된 필드와 유지된 필드 전수 검증")
    void updateMyInfo_Success() throws JsonProcessingException {
        // Given: 수정 요청
        String newName = "김철수";
        String newContact = "010-0000-0000";
        var updateReq = new MemberDto.MemberUpdateRequest(newName, newContact);

        // When
        assertThat(mvc.patch().uri("/api/members/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(updateReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(MemberDto.MemberResponse.class)
                .satisfies(res -> {
                    // 수정된 필드 확인
                    assertThat(res.name()).isEqualTo(newName);
                    assertThat(res.contact()).isEqualTo(newContact);
                    // 불변 필드 확인
                    assertThat(res.loginId()).isEqualTo(LOGIN_ID);
                    assertThat(res.externalId()).isEqualTo(authenticatedExternalId);
                });
    }

    @Test
    @DisplayName("성공: 회원 탈퇴 후 상태값 WITHDRAWN 변경 확인")
    void withdraw_Success() {
        // When: 탈퇴 실행
        assertThat(mvc.delete().uri("/api/members/me")
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK);

        // Then: 탈퇴 후 재조회 시 상태값 검증
        assertThat(mvc.get().uri("/api/members/me")
                .header("Authorization", "Bearer " + accessToken))
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> {
                    assertThat(res.code()).isEqualTo("M004");
                    assertThat(res.errorName()).contains("MEMBER_NOT_ACTIVE");
                });
    }

    // --- 2. 추가 예외 상황 테스트 ---
    @Test
    @DisplayName("실패: 유효하지 않은 토큰으로 접근 시 403 Forbidden 발생")
    void getMyInfo_Fail_InvalidToken() {
        assertThat(mvc.get().uri("/api/members/me")
                .header("Authorization", "Bearer WRONG_TOKEN"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }
}