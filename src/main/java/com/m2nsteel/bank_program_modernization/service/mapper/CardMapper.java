package com.m2nsteel.bank_program_modernization.service.mapper;

import com.m2nsteel.bank_program_modernization.domain.*;
import com.m2nsteel.bank_program_modernization.usecase.CardUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardMapper {
    @Mapping(target = "cardNumber", source = "card.cardNumber", qualifiedByName = "maskCard")
    @Mapping(target = "accountNumber", source = "card.account.accountNumber")
    CardUsecase.CardResult toResult(Card card);

    @Mapping(target = "paymentExternalId", source = "payment.externalId")
    @Mapping(target = "transactionExternalId", source = "transaction.externalId")
    @Mapping(target = "maskedCardNumber", expression = "java(maskCardNumber(card.getCardNumber()))")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "balanceAfter", source = "cardAccount.balance")
    @Mapping(target = "accountNumber", source = "cardAccount.accountNumber")
    @Mapping(target = "amount", source = "payment.amount")
    @Mapping(target = "createdAt", source = "payment.createdAt")
    CardUsecase.CardPaymentResult toPaymentResult(
            Payment payment,
            Transaction transaction,
            Card card,
            Account cardAccount,
            String merchantName
    );

    @Mapping(target = "refundExternalId", source = "refund.externalId")
    @Mapping(target = "refundTxExternalId", source = "refundTx.externalId")
    @Mapping(target = "originalPaymentExternalId", source = "payment.externalId")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "originalAmount", source = "payment.amount")
    @Mapping(target = "remainingAmount", expression = "java(payment.getRefundableAmount())")
    @Mapping(target = "createdAt", source = "refund.createdAt")
    CardUsecase.RefundResult toRefundResult(Payment payment, Refund refund, Transaction refundTx);

    @Named("maskCard")
    default String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(12);
    }

    @Mapping(target = "paymentExternalId", source = "payment.externalId")
    @Mapping(target = "maskedCardNumber", expression = "java(maskCardNumber(card.getCardNumber()))")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "createdAt", source = "payment.createdAt")
    CardUsecase.PaymentSummary toPaymentSummary(
            Payment payment,
            Card card,
            String merchantName
    );
}
