package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.AccountDto;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountDtoMapper {
    AccountUsecase.AccountCreateCommand toCommand(AccountDto.AccountCreateRequest request, String memberExternalId);
    AccountDto.AccountResponse from(AccountUsecase.AccountResult result);
}
