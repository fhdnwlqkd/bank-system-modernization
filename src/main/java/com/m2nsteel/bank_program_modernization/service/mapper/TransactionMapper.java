package com.m2nsteel.bank_program_modernization.service.mapper;

import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    @Mapping(target = "externalId", source = "transaction.externalId")
    @Mapping(target = "accountNumber", source = "item.account.accountNumber")
    @Mapping(target = "balanceAfter", source = "item.balanceAfter")
    TransactionUsecase.GeneralResult toResult(Transaction transaction, TransactionItem item);

    @Mapping(target = "externalId", source = "transaction.externalId")
    @Mapping(target = "fromAccountNumber", source = "fromItem.account.accountNumber")
    @Mapping(target = "toAccountNumber", source = "toItem.account.accountNumber")
    @Mapping(target = "balanceAfter", source = "fromItem.balanceAfter")
    TransactionUsecase.TransferResult toTransferResult(Transaction transaction, TransactionItem fromItem, TransactionItem toItem);
}
