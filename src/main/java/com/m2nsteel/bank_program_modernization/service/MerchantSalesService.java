package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.repository.merchant.MerchantRepository;
import com.m2nsteel.bank_program_modernization.usecase.CursorResult;
import com.m2nsteel.bank_program_modernization.usecase.MerchantUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantSalesService {

    private final MemberRepository memberRepository;
    private final MerchantRepository merchantPaymentRepository;

    /**
     * 가맹점 매출 요약 집계
     */
    public MerchantUsecase.MerchantSalesSummary getSettlementSummary(String merchantExternalId, LocalDate from, LocalDate to) {
        // 1. 가맹점 소유주 확인
        memberRepository.findByExternalId(merchantExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var condition = new MerchantUsecase.MerchantPaymentSearchCondition(
                merchantExternalId, PaymentStatus.SUCCESS, from, to, null, 0);
        return merchantPaymentRepository.getSalesSummary(condition);
    }

    /**
     * 가맹점 결제 내역 커서 페이징 조회
     */
    public CursorResult<MerchantUsecase.MerchantPaymentResult> getPayments(
            String merchantExternalId, MerchantUsecase.MerchantPaymentSearchCondition cond) {
        // 1. 가맹점 소유주 확인
        memberRepository.findByExternalId(merchantExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. PageRequest 생성
        PageRequest pageable = PageRequest.of(0, cond.size());

        // 3. Slice 조회 수행
        Slice<MerchantUsecase.MerchantPaymentResult> slice = merchantPaymentRepository.searchPayments(cond, pageable);

        // 4. 커서 결과 가공
        Long nextCursor = slice.isEmpty() ? null :
                slice.getContent().get(slice.getNumberOfElements() - 1).paymentId();

        return new CursorResult<>(slice.getContent(), nextCursor, slice.hasNext());
    }
}