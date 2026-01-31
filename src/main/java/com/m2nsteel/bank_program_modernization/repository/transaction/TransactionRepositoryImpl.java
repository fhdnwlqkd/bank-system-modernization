package com.m2nsteel.bank_program_modernization.repository.transaction;

import com.m2nsteel.bank_program_modernization.domain.*;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<TransactionItem> search(TransactionQueryCriteria criteria, Pageable pageable) {
        QTransaction transaction = QTransaction.transaction;
        QTransactionItem item = QTransactionItem.transactionItem;

        // 1. TransactionItem 조회
        List<TransactionItem> content = queryFactory
                .selectFrom(item)
                .join(item.transaction, transaction).fetchJoin()
                .where(
                        accountExternalIdEq(criteria.accountExternalId()),
                        ltLastId(criteria.lastId()),
                        dateBetween(criteria.startDate(), criteria.endDate()),
                        typeEq(criteria.type()),
                        amountBetween(criteria.minAmount(), criteria.maxAmount())
                )
                .limit(pageable.getPageSize() + 1)
                .orderBy(item.id.desc())
                .fetch();

        // 2. 다음 페이지 존재 여부 확인 및 데이터 정제
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression ltLastId(Long lastId) {
        return lastId != null ? QTransactionItem.transactionItem.id.lt(lastId) : null;
    }

    // --- Null-Safe 조건절 ---

    private BooleanExpression accountExternalIdEq(String externalId) {
        return externalId != null ? QTransactionItem.transactionItem.account.externalId.eq(externalId) : null;
    }

    private BooleanExpression dateBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return null;
        return QTransaction.transaction.createdAt.between(start.atStartOfDay(), end.atTime(LocalTime.MAX));
    }

    private BooleanExpression typeEq(TransactionType type) {
        return type != null ? QTransaction.transaction.type.eq(type) : null;
    }

    private BooleanExpression amountBetween(Long min, Long max) {
        if (min == null && max == null) return null;
        if (min != null && max != null) return QTransaction.transaction.amount.between(min, max);
        if (min != null) return QTransaction.transaction.amount.goe(min);
        return QTransaction.transaction.amount.loe(max);
    }
}