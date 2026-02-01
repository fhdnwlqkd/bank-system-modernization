package com.m2nsteel.bank_program_modernization.controller.mapper;

import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.usecase.AuthUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthDtoMapper {
    AuthUsecase.LoginCommand toCommand(AuthDto.LoginRequest request);
    AuthDto.TokenResponse from(AuthUsecase.TokenResult result);
}
