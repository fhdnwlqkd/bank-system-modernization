package com.m2nsteel.bank_program_modernization.domain.validator;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyValidator {
    private final TransactionRepository transactionRepository;
    public void verify(String requestId) {
        if (transactionRepository.existsByRequestId(requestId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
    }
}
