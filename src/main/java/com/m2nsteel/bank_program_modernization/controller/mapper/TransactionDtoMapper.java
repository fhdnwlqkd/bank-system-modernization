package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.TransactionDto;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionDtoMapper {
    TransactionUsecase.DepositCommand toCommand(TransactionDto.DepositRequest request);
    TransactionUsecase.WithdrawCommand toCommand(TransactionDto.WithdrawRequest request);
    TransactionUsecase.TransferCommand toCommand(TransactionDto.TransferRequest request);
    TransactionUsecase.TransactionSearchCondition toCondition(TransactionDto.TransactionSearchRequest request);
    TransactionDto.GeneralResponse from(TransactionUsecase.GeneralResult result);
    TransactionDto.TransferResponse from(TransactionUsecase.TransferResult result);
    TransactionDto.TransactionHistoryResponse from(TransactionUsecase.TransactionHistoryResult result);
}
