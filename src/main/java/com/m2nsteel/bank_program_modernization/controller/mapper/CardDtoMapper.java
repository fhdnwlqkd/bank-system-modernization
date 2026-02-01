package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.CardDto;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardDtoMapper {
    CardUsecase.IssueCardCommand toCommand(CardDto.CardIssueRequest request);
    CardUsecase.CardPaymentCommand toCommand(CardDto.CardPaymentRequest request);
    CardUsecase.RefundCommand toCommand(CardDto.RefundRequest request);
    CardDto.CardResponse from(CardUsecase.CardResult result);
    CardDto.CardPaymentResponse from(CardUsecase.CardPaymentResult result);
    CardDto.RefundResponse from(CardUsecase.RefundResult result);
}
