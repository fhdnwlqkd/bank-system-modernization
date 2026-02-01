package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.MerchantDto;
import com.m2nsteel.bank_program_modernization.usecase.MerchantUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MerchantDtoMapper {
    @Mapping(target = "merchantExternalId", source = "externalId")
    MerchantUsecase.MerchantPaymentSearchCondition toCondition(MerchantDto.MerchantPaymentSearchRequest request, String externalId);
    MerchantDto.PaymentHistoryResponse from(MerchantUsecase.MerchantPaymentResult result);
    MerchantDto.SalesSummaryResponse from(MerchantUsecase.MerchantSalesSummary result);
}
