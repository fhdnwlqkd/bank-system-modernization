package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.core.security.TokenProvider;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.request.LoginRequest;
import com.m2nsteel.bank_program_modernization.dto.response.TokenResponse;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    /*
    로그인 처리 및 토큰 발급
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 사용자 존재 여부 및 비밀번호 확인
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 2. 계좌 상태 확인 (정상 상태만 로그인 가능)
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        // 3. 토큰 생성
        String accessToken = tokenProvider.createAccessToken(member.getLoginId(), member.getRole().name());
        String refreshToken = tokenProvider.createRefreshToken(member.getLoginId());

        return new TokenResponse(accessToken, refreshToken);
    }

    /*
    본인 인증 확인
     */
    public void verifyMember(Long memberId, String password) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
