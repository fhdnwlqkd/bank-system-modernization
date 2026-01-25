package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);
    @Query(value = "SELECT nextval('member_num_seq')", nativeQuery = true)
    Long getNextMemberSequence();
}
