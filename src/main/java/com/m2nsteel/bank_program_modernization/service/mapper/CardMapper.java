package com.m2nsteel.bank_program_modernization.service.mapper;

import com.m2nsteel.bank_program_modernization.domain.*;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardMapper {
    @Mapping(target = "externalId", source = "card.externalId")
    @Mapping(target = "cardNumber", expression = "java(maskCardNumber(card.getCardNumber()))")
    CardUsecase.CardResult toResult(Card card);

    @Mapping(target = "paymentExternalId", source = "payment.externalId")
    @Mapping(target = "transactionExternalId", source = "transaction.externalId")
    @Mapping(target = "maskedCardNumber", expression = "java(maskCardNumber(card.getCardNumber()))")
    @Mapping(target = "balanceAfter", source = "currentBalance")
    @Mapping(target = "merchantName", source = "payment.merchantAccount.member.name")
    @Mapping(target = "status", source = "transaction.status")
    @Mapping(target = "amount", source = "payment.amount")
    CardUsecase.CardPaymentResult toPaymentResult(
            Payment payment,
            Transaction transaction,
            Card card,
            Long currentBalance
    );

    @Mapping(target = "refundTransactionExternalId", source = "refundTransaction.externalId")
    @Mapping(target = "originalPaymentExternalId", source = "payment.externalId")
    @Mapping(target = "refundAmount", source = "refundTransaction.amount")
    @Mapping(target = "totalRefundedAmount", source = "payment.refundedAmount")
    @Mapping(target = "remainingAmount", expression = "java(payment.getRefundableAmount())")
    @Mapping(target = "occurredAt", source = "refundTransaction.createdAt")
    CardUsecase.RefundResult toRefundResult(Payment payment, Transaction refundTransaction);

    default String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(12);
    }
}
