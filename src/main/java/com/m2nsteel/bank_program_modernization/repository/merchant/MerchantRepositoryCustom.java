package com.m2nsteel.bank_program_modernization.repository.merchant;

import com.m2nsteel.bank_program_modernization.usecase.MerchantUsecase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MerchantRepositoryCustom {
    MerchantUsecase.MerchantSalesSummary getSalesSummary(MerchantUsecase.MerchantPaymentSearchCondition cond);
    Slice<MerchantUsecase.MerchantPaymentResult> searchPayments(MerchantUsecase.MerchantPaymentSearchCondition cond, Pageable pageable);
}
