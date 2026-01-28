package com.m2nsteel.bank_program_modernization.usecase.mapper;

import com.m2nsteel.bank_program_modernization.domain.Admin;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.Merchant;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberMapper {
    MemberUsecase.MemberResult toResult(Member member);
    MemberUsecase.AdminResult toResult(Admin admin);
    MemberUsecase.MerchantResult toResult(Merchant merchant);
}
