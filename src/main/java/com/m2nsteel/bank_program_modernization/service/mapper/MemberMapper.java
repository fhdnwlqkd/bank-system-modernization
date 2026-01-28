package com.m2nsteel.bank_program_modernization.service.mapper;

import com.m2nsteel.bank_program_modernization.domain.Admin;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.Merchant;
import com.m2nsteel.bank_program_modernization.dto.result.AdminResult;
import com.m2nsteel.bank_program_modernization.dto.result.MemberResult;
import com.m2nsteel.bank_program_modernization.dto.result.MerchantResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE // 매핑되지 않는 필드는 무시 (보안상 안전)
)
public interface MemberMapper {

    // Entity -> Result 자동 매핑
    MemberResult toResult(Member member);

    MerchantResult toResult(Merchant merchant);

    AdminResult toResult(Admin admin);
}
