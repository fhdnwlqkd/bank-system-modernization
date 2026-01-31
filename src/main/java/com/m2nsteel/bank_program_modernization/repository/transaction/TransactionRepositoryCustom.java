package com.m2nsteel.bank_program_modernization.repository.transaction;

import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TransactionRepositoryCustom {
    Slice<TransactionItem> search(TransactionQueryCriteria cond, Pageable pageable);
}
