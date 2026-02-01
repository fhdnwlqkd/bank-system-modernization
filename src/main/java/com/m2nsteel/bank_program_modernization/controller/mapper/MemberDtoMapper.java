package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberDtoMapper {
    MemberUsecase.MemberSignUpCommand toCommand(MemberDto.MemberSignUpRequest request);
    MemberUsecase.MerchantSignUpCommand toCommand(MemberDto.MerchantSignUpRequest request);
    MemberUsecase.AdminSignUpCommand toCommand(MemberDto.AdminSignUpRequest request);

    MemberDto.MemberResponse from(MemberUsecase.MemberResult result);
    MemberDto.MerchantSignUpResponse from(MemberUsecase.MerchantSignUpResult result);
    MemberDto.AdminResponse from(MemberUsecase.AdminResult result);
}
