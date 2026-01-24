package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.MemberResponse;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /*
      회원 가입
     */
    @Transactional
    public MemberResponse signUp(MemberSignUpRequest request) {

        // 1. 아이디 중복 체크
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 회원 엔티티 생성 및 저장
        Member member = Member.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .memberNumber(request.MemberNumber())
                .name(request.name())
                .branchId(request.branchId())
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Member savedMember = memberRepository.save(member);

        // 4. 응답 DTO 변환
        return new MemberResponse(
                savedMember.getId(),
                savedMember.getLoginId(),
                savedMember.getMemberNumber(),
                savedMember.getName()
        );
    }
}
