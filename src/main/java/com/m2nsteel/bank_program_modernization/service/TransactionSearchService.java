package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionQueryCriteria;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionRepository;
import com.m2nsteel.bank_program_modernization.service.mapper.TransactionMapper;
import com.m2nsteel.bank_program_modernization.usecase.CursorResult;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionSearchService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    /**
     * 내 계좌의 거래 내역 검색 (동적 조건 및 페이징)
     */
    public CursorResult<TransactionUsecase.TransactionHistoryResult> searchTransactions(
            String requesterExternalId,
            boolean isAdmin,
            TransactionUsecase.TransactionSearchCondition condition,
            Pageable pageable) {

        // 1. 계좌 확인 및 인가 체크
        Account account = accountRepository.findByExternalId(condition.accountExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!isAdmin && !account.getMember().getExternalId().equals(requesterExternalId)) {
            throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
        }

        // 2. Criteria 변환
        TransactionQueryCriteria criteria = transactionMapper.toCriteria(condition);

        // 3. Slice 조회 및 변환
        Slice<TransactionItem> slice = transactionRepository.search(criteria, pageable);
        List<TransactionUsecase.TransactionHistoryResult> values = slice.getContent().stream()
                .map(transactionMapper::toHistoryResult)
                .toList();

        // 4. 다음 커서 계산 (리스트의 마지막 데이터 ID)
        Long nextCursor = values.isEmpty() ? null :
                slice.getContent().get(values.size() - 1).getId();

        return new CursorResult<>(values, nextCursor, slice.hasNext());
    }
}
