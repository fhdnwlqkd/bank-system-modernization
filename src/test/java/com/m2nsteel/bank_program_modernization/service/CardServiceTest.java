package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
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

    @BeforeEach
    void setUp() {
        // 1. 지점 생성
        var branchResponse = branchService.createBranch(
                new com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest(
                        "Test Branch", "123 Test St", "555-0000"
                )
        );

        // 2. 테스트용 회원 가입
        var memberResponse = memberService.signUp(new MemberSignUpRequest(
                "tester", "pw123", "M123", branchResponse.branchCode()));
        memberId = memberResponse.memberId();

        // 3. 테스트용 계좌 생성
        var accountResponse = accountService.createAccount(new AccountCreateRequest(
                memberResponse.memberNumber(), branchResponse.branchCode(), "1234"
        ));
        accountNumber = accountResponse.accountNumber();
    }

    @Test
    @DisplayName("카드 발급 성공 테스트")
    void createCard_success() {
        // Given
        CardCreateRequest request = new CardCreateRequest(accountNumber, "1234", "CHECK");

        // When
        var response = cardService.createCard(request);

        // Then
        assertThat(response.accountNumber()).isEqualTo(accountNumber);
        assertThat(response.cardType()).isEqualTo("CHECK");

        // 마스킹 검증 (9410-****-****-XXXX 형태인지)
        assertThat(response.maskedCardNumber()).contains("-****-****-");
        assertThat(response.maskedCardNumber().length()).isEqualTo(19);

        // DB 저장 확인
        assertThat(cardRepository.findAll()).hasSize(1);
    }

//    @Test
//    @DisplayName("카드 발급 실패: 본인 계좌가 아님")
//    void createCard_fail_notOwner() {
//        // Given: 다른 사용자의 ID (memberId + 999)
//        Long wrongMemberId = memberId + 999L;
//        CardCreateRequest request = new CardCreateRequest(accountNumber, "1234", "CHECK");
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class, () -> {
//            cardService.createCard(request, wrongMemberId);
//        });
//
//        // 에러 코드 확인 (미리 정의한 ErrorCode에 따라 다를 수 있음)
//        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_ACCOUNT_OWNER);
//    }

    @Test
    @DisplayName("카드 발급 실패: 존재하지 않는 계좌")
    void createCard_fail_accountNotFound() {
        // Given: 가짜 계좌 번호
        CardCreateRequest request = new CardCreateRequest("999-999-999", "1234", "CHECK");

        // When & Then
        assertThrows(BusinessException.class, () -> {
            cardService.createCard(request);
        });
    }
}