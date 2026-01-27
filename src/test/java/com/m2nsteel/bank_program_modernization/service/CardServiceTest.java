package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.CardCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CardServiceTest {

    @Autowired CardService cardService;
    @Autowired MemberService memberService;
    @Autowired AccountService accountService;
    @Autowired BranchService branchService;
    @Autowired CardRepository cardRepository;

    private Long memberId;
    private String accountNumber;

    private String memberLoginId = "member1";
    private String accountPassword = "password1234";
    private String wrongLoginId = "wrongMember";


    @BeforeEach
    void setUp() {
        // 1. 지점 생성
        var branch = branchService.createBranch(
                new BranchCreateRequest("Branch1", "Address1", "Contact1")
        );
        // 2.가입 및 계좌 생성
        var member = memberService.signUp(new MemberSignUpRequest(memberLoginId, "p1", "Member", branch.branchCode()));

        var account = accountService.createAccount(new AccountCreateRequest(member.memberNumber(), branch.branchCode(), accountPassword));
        accountNumber = account.accountNumber();
        var wrongMember = memberService.signUp(new MemberSignUpRequest(wrongLoginId, "p2", "Wrong Member", branch.branchCode()));
    }

    @Test
    @DisplayName("카드 발급 성공 테스트")
    void createCard_success() {
        // Given
        CardCreateRequest request = new CardCreateRequest(accountNumber, "1234", "CHECK");

        // When
        var response = cardService.createCard(request, memberLoginId);

        // Then
        assertThat(response.accountNumber()).isEqualTo(accountNumber);
        assertThat(response.cardType()).isEqualTo("CHECK");

        // 마스킹 검증 (9410-****-****-XXXX 형태인지)
        assertThat(response.maskedCardNumber()).contains("-****-****-");
        assertThat(response.maskedCardNumber().length()).isEqualTo(19);

        // DB 저장 확인
        assertThat(cardRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("카드 발급 실패: 본인 계좌가 아님")
    void createCard_fail_notOwner() {
        CardCreateRequest request = new CardCreateRequest(accountNumber, "1234", "CHECK");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            cardService.createCard(request, wrongLoginId);
        });

        // 에러 코드 확인 (미리 정의한 ErrorCode에 따라 다를 수 있음)
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
    }

    @Test
    @DisplayName("카드 발급 실패: 존재하지 않는 계좌")
    void createCard_fail_accountNotFound() {
        // Given: 가짜 계좌 번호
        CardCreateRequest request = new CardCreateRequest("999-999-999", "1234", "CHECK");

        // When & Then
        assertThrows(BusinessException.class, () -> {
            cardService.createCard(request, memberLoginId);
        });
    }
}