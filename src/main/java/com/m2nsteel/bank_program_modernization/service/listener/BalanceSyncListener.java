package com.m2nsteel.bank_program_modernization.service.listener;

import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceSyncListener {
    private final AccountRepository accountRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceSync(BalanceSyncEvent event) {
        accountRepository.updateBalance(event.accountId(), event.balance());
        log.debug("DB 잔액 동기화 완료: Account[{}] -> {}원", event.accountId(), event.balance());
    }
}
