package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.MerchantMember;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MerchantSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.MemberResponse;
import com.m2nsteel.bank_program_modernization.dto.response.MerchantSignUpResponse;
import com.m2nsteel.bank_program_modernization.repository.BranchRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    /*
      회원 가입
     */
    @Transactional
    public MemberResponse signUp(MemberSignUpRequest request) {

        // 1. 아이디 중복 체크 및 지점 조회
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        Branch branch = branchRepository.findByBranchCode(request.branchCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_NOT_FOUND));

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 회원 번호 생성
        Long seq = memberRepository.getNextMemberSequence();
        String memberNumber = String.format("M-%d-%06d", LocalDate.now().getYear(), seq);

        // 4. 회원 엔티티 생성 및 저장
        Member member = Member.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .memberNumber(memberNumber)
                .name(request.name())
                .branchId(branch.getId())
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Member savedMember = memberRepository.save(member);

        // 5. 응답 DTO 변환
        return new MemberResponse(
                savedMember.getId(),
                savedMember.getLoginId(),
                savedMember.getMemberNumber(),
                savedMember.getName()
        );
    }

    /*
        가맹점 회원 가입
     */
    public MerchantSignUpResponse merchantSignUp(MerchantSignUpRequest request) {
        // 1. 아이디 중복 체크 및 지점 조회
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        Branch branch = branchRepository.findByBranchCode(request.branchCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_NOT_FOUND));

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 회원 번호 생성
        Long seq = memberRepository.getNextMemberSequence();
        String memberNumber = String.format("MER-%d-%06d", LocalDate.now().getYear(), seq);

        // 4. 회원 엔티티 생성 및 저장
        MerchantMember merchantMember = MerchantMember.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .memberNumber(memberNumber)
                .name(request.merchantName())
                .branchId(branch.getId())
                .role(MemberRole.MERCHANT)
                .status(MemberStatus.ACTIVE)
                .businessRegistrationNumber(request.businessRegistrationNumber())
                .merchantCategory(request.merchantCategory())
                .build();

        MerchantMember savedMember = memberRepository.save(merchantMember);

        // 5. 응답 DTO 변환
        return new MerchantSignUpResponse(
                savedMember.getId(),
                savedMember.getLoginId(),
                savedMember.getMemberNumber(),
                merchantMember.getName(),
                merchantMember.getBusinessRegistrationNumber(),
                merchantMember.getMerchantCategory()
        );
    }

    public MemberResponse getMemberInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return new MemberResponse(
                member.getId(),
                member.getLoginId(),
                member.getMemberNumber(),
                member.getName()
        );
    }
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        // Spring Security의 User 객체로 변환하여 반환
        return User.builder()
                .username(member.getLoginId())
                .password(member.getPassword())
                .roles(member.getRole().name())
                .build();
    }
}
