package com.m2nsteel.bank_program_modernization.service.transaction;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionRepository;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.service.TransactionService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
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
class TransactionServiceTest {

    @Autowired private TransactionService transactionService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    private String senderExternalId;
    private String receiverExternalId;
    private String senderAccountNo;
    private String receiverAccountNo;
    private final String PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        // 1. 회원 생성
        var sMember = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "sender", PASSWORD, "송금인", "010-1111-2222"));
        var rMember = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "receiver", PASSWORD, "수취인", "010-3333-4444"));

        senderExternalId = sMember.externalId();
        receiverExternalId = rMember.externalId();

        // 2. 계좌 생성
        var sAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(
                senderExternalId, PASSWORD));
        var rAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(
                receiverExternalId, PASSWORD));

        senderAccountNo = sAcc.accountNumber();
        receiverAccountNo = rAcc.accountNumber();
    }

    @Test
    @DisplayName("성공: 입금 후 이체 프로세스 - 멱등성 확인 및 최종 잔액 검증")
    void full_Banking_Process_Success() {
        // 1. Given: 초기 잔액 입금
        String depositKey = "deposit-key-100";
        transactionService.deposit(new TransactionUsecase.DepositCommand(
                senderAccountNo, 10000L, depositKey), senderExternalId);

        // 2. When: 이체 수행 (5,000원)
        String transferKey = "transfer-key-500";
        var command = new TransactionUsecase.TransferCommand(
                senderAccountNo, receiverAccountNo, 5000L, PASSWORD, transferKey);

        var firstResult = transactionService.transfer(command, senderExternalId);

        // 3. Then: 첫 요청 결과 검증
        assertThat(firstResult.isRepeated()).isFalse();
        assertThat(firstResult.amount()).isEqualTo(5000L);

        // 4. When: 동일한 키로 중복 이체 요청 (Idempotency 재시도)
        var secondResult = transactionService.transfer(command, senderExternalId);

        // 5. Then: 중복 요청 결과 검증
        assertThat(secondResult.isRepeated()).isTrue();
        assertThat(secondResult.txExternalId()).isEqualTo(firstResult.txExternalId());

        // 6. Then: 최종 원장 잔액 확인
        Account senderFinal = accountRepository.findByAccountNumber(senderAccountNo).orElseThrow();
        Account receiverFinal = accountRepository.findByAccountNumber(receiverAccountNo).orElseThrow();

        assertThat(senderFinal.getBalance()).isEqualTo(5000L); // 10,000 - 5,000
        assertThat(receiverFinal.getBalance()).isEqualTo(5000L); // 0 + 5,000
    }

    @Test
    @DisplayName("성공: 출금 로직 검증 및 비밀번호 확인")
    void withdraw_Process_Success() {
        // 1. Given: 10,000원 먼저 입금
        transactionService.deposit(new TransactionUsecase.DepositCommand(
                senderAccountNo, 10000L, "idemp-d-1"), senderExternalId);

        // 2. When: 3,000원 출금
        var withdrawCmd = new TransactionUsecase.WithdrawCommand(
                senderAccountNo, 3000L, PASSWORD, "idemp-w-1");
        var result = transactionService.withdraw(withdrawCmd, senderExternalId);

        // 3. Then: 결과 확인
        assertThat(result.amount()).isEqualTo(3000L);
        assertThat(result.isRepeated()).isFalse();

        Account finalAcc = accountRepository.findByAccountNumber(senderAccountNo).orElseThrow();
        assertThat(finalAcc.getBalance()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("실패: 잔액 부족 시 이체 실패 (예외 발생)")
    void transfer_InsufficientBalance_Fail() {
        // 1. Given: 잔액은 0원인 상태

        // 2. When & Then: 5,000원 이체 시도 시 예외 발생 확인
        var command = new TransactionUsecase.TransferCommand(
                senderAccountNo, receiverAccountNo, 5000L, PASSWORD, "idemp-fail-1");

        assertThatThrownBy(() -> transactionService.transfer(command, senderExternalId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("실패: 회원 불일치 이체 실패 (예외 발생)")
    void withdraw_Unauthorized_Access() {
        // When & Then: senderAccountNo로 receiver가 입금 시도 시 예외 발생 확인
        var depositCmd = new TransactionUsecase.DepositCommand(
                senderAccountNo, 5000L, "idemp-unauth-1");
        assertThatThrownBy(() -> transactionService.deposit(depositCmd, receiverExternalId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCOUNT_OWNER);
    }
}
