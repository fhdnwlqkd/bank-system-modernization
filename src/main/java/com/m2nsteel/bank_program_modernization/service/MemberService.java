package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Admin;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.Merchant;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.service.mapper.MemberMapper;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 1. 일반 회원 가입 (Member)
     */
    @Transactional
    public MemberUsecase.MemberResult signUp(MemberUsecase.MemberSignUpCommand command) {
        validateDuplicateLoginId(command.loginId());

        String encodedPassword = passwordEncoder.encode(command.password());

        Member member = Member.create(
                command.loginId(),
                encodedPassword,
                command.name(),
                command.contact()
        );

        return memberMapper.toResult(memberRepository.save(member));
    }

    /**
     * 2. 가맹점 회원 가입 (Merchant)
     */
    @Transactional
    public MemberUsecase.MerchantResult merchantSignUp(MemberUsecase.MerchantSignUpCommand command) {
        validateDuplicateLoginId(command.loginId());

        String encodedPassword = passwordEncoder.encode(command.password());

        Merchant merchant = Merchant.create(
                command.loginId(),
                encodedPassword,
                command.name(),
                command.contact(),
                command.businessNumber(),
                command.shopName(),
                command.category()
        );

        Merchant savedMerchant = memberRepository.save(merchant);
        createMerchantAccount(savedMerchant);

        return memberMapper.toResult(savedMerchant);
    }

    /**
     * 3. 관리자 회원 가입 (Admin)
     */
    @Transactional
    public MemberUsecase.AdminResult adminSignUp(MemberUsecase.AdminSignUpCommand command) {
        validateDuplicateLoginId(command.loginId());

        String encodedPassword = passwordEncoder.encode(command.password());

        Admin admin = Admin.create(
                command.loginId(),
                encodedPassword,
                command.name(),
                command.contact(),
                command.department()
        );

        return memberMapper.toResult(memberRepository.save(admin));
    }

    /**
     * 내 정보 수정
     */
    @Transactional
    public MemberUsecase.MemberResult updateMyInfo(String externalId, MemberUsecase.MemberUpdateCommand command) {
        Member member = findMemberOrThrow(externalId);

        String encodedPassword = encodePasswordIfPresent(command.password());
        member.updateInfo(command.name(), command.contact(), encodedPassword);

        return memberMapper.toResult(member);
    }

    /**
     * 가맹점 정보 수정
     */
    @Transactional
    public MemberUsecase.MerchantResult updateMerchantInfo(String externalId, MemberUsecase.MerchantUpdateCommand command) {
        Member member = findMemberOrThrow(externalId);

        // 안전한 타입 캐스팅 검증
        if (!(member instanceof Merchant merchant)) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_TYPE);
        }

        String encodedPassword = encodePasswordIfPresent(command.password());
        merchant.updateInfo(command.name(), command.contact(), encodedPassword);
        merchant.updateMerchant(command.shopName(), command.category());

        return memberMapper.toResult(merchant);
    }

    /**
     * 관리자 정보 수정
     */
    @Transactional
    public MemberUsecase.AdminResult updateAdminInfo(String externalId, MemberUsecase.AdminUpdateCommand command) {
        Member member = findMemberOrThrow(externalId);

        // 안전한 타입 캐스팅 검증
        if (!(member instanceof Admin admin)) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_TYPE);
        }

        String encodedPassword = encodePasswordIfPresent(command.password());
        admin.updateInfo(command.name(), command.contact(), encodedPassword);
        admin.updateAdmin(command.department());

        return memberMapper.toResult(admin);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(String externalId) {
        Member member = findMemberOrThrow(externalId);
        member.withdraw();
    }

    /**
     * [Admin] 멤버 목록 조회
     */
//    public Page<MemberUsecase.MemberResult> getMembersByAdmin(MemberSearchCondition condition, Pageable pageable) {}

    public MemberUsecase.MemberResult getMemberInfo(String externalId) {
        return memberMapper.toResult(findMemberOrThrow(externalId));
    }

    // --- 헬퍼 메서드 ---

    private Member findMemberOrThrow(String externalId) {
        return memberRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    private void createMerchantAccount(Merchant merchant) {
        Account account = Account.create(
                merchant.getPassword(),
                merchant
        );
        accountRepository.save(account);
    }

    private @Nullable String encodePasswordIfPresent(@Nullable String rawPassword) {
        if (StringUtils.hasText(rawPassword)) {
            return passwordEncoder.encode(rawPassword);
        }
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        return User.builder()
                .username(member.getLoginId())
                .password(member.getPassword())
                .roles(member.getRole().name())
                .build();
    }
}
