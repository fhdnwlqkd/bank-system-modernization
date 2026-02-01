package com.m2nsteel.bank_program_modernization.service.mapper;

import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionQueryCriteria;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    @Mapping(target = "txExternalId", source = "transaction.externalId")
    @Mapping(target = "accountNumber", source = "item.account.accountNumber")
    @Mapping(target = "balanceAfter", source = "item.balanceAfter")
    @Mapping(target = "createdAt", source = "transaction.createdAt")
    TransactionUsecase.GeneralResult toResult(Transaction transaction, TransactionItem item);

    @Mapping(target = "txExternalId", source = "item.transaction.externalId")
    @Mapping(target = "type", source = "item.transaction.type")
    @Mapping(target = "amount", source = "item.transaction.amount")
    @Mapping(target = "delta", source = "item.delta")
    @Mapping(target = "balanceAfter", source = "item.balanceAfter")
    @Mapping(target = "createdAt", source = "item.transaction.createdAt")
    TransactionUsecase.TransactionHistoryResult toHistoryResult(TransactionItem item);

    @Mapping(target = "txExternalId", source = "transaction.externalId")
    @Mapping(target = "fromAccountNumber", source = "fromItem.account.accountNumber")
    @Mapping(target = "toAccountNumber", source = "toItem.account.accountNumber")
    @Mapping(target = "balanceAfter", source = "fromItem.balanceAfter")
    @Mapping(target = "createdAt", source = "transaction.createdAt")
    TransactionUsecase.TransferResult toTransferResult(Transaction transaction, TransactionItem fromItem, TransactionItem toItem);

    TransactionQueryCriteria toCriteria(TransactionUsecase.TransactionSearchCondition condition);
}
