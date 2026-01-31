package com.m2nsteel.bank_program_modernization.repository.merchant;

import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;
import com.m2nsteel.bank_program_modernization.usecase.MerchantUsecase;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.m2nsteel.bank_program_modernization.domain.QPayment.payment;

@RequiredArgsConstructor
public class MerchantRepositoryImpl implements MerchantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public MerchantUsecase.MerchantSalesSummary getSalesSummary(MerchantUsecase.MerchantPaymentSearchCondition cond) {

        // 1. 총 결제 원금 합계 (발생한 모든 매출의 합)
        NumberExpression<Long> totalSalesAmount = payment.amount.sum().coalesce(0L);

        // 2. 총 누적 환불액 합계 (부분 환불 + 전체 환불 포함)
        NumberExpression<Long> totalRefundedAmount = payment.totalRefundedAmount.sum().coalesce(0L);

        // 3. 유효 거래 건수
        NumberExpression<Long> activeTransactionCount = new CaseBuilder()
                .when(payment.status.ne(PaymentStatus.REFUNDED))
                .then(1L)
                .otherwise(0L)
                .sum().coalesce(0L);

        return queryFactory
                .select(Projections.constructor(MerchantUsecase.MerchantSalesSummary.class,
                        totalSalesAmount,              // totalSalesAmount (원금 합계)
                        activeTransactionCount,        // totalTransactionCount (실제 판매 중인 건수)
                        totalRefundedAmount,           // totalRefundAmount (환불된 총액)
                        totalSalesAmount.subtract(totalRefundedAmount) // netAmount (순매출)
                ))
                .from(payment)
                .where(
                        payment.merchant.externalId.eq(cond.merchantExternalId()),
                        dateBetween(cond.from(), cond.to())
                )
                .fetchOne();
    }

    @Override
    public Slice<MerchantUsecase.MerchantPaymentResult> searchPayments(MerchantUsecase.MerchantPaymentSearchCondition cond, Pageable pageable) {
        List<MerchantUsecase.MerchantPaymentResult> content = queryFactory
                .select(Projections.constructor(MerchantUsecase.MerchantPaymentResult.class,
                        payment.id,
                        payment.externalId,
                        payment.amount,
                        payment.status,
                        payment.createdAt,
                        payment.card.cardNumber
                ))
                .from(payment)
                .where(
                        payment.merchant.externalId.eq(cond.merchantExternalId()),
                        statusEq(cond.status()),
                        dateBetween(cond.from(), cond.to()),
                        ltPaymentId(cond.lastId())
                )
                .orderBy(payment.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // --- Helper Methods ---
    private BooleanExpression ltPaymentId(Long lastId) {
        return lastId != null ? payment.id.lt(lastId) : null;
    }

    private BooleanExpression statusEq(PaymentStatus status) {
        return status != null ? payment.status.eq(status) : null;
    }

    private BooleanExpression dateBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return null;
        return payment.createdAt.between(from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }
}