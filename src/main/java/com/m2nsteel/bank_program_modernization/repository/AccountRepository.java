package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a " +
            "JOIN a.member m " +
            "WHERE m.businessNumber = :businessNumber " +
            "AND a.status = ACTIVE")
    Optional<Account> findByBusinessNumber(@Param("businessNumber") String businessNumber);
    boolean existsByMember(Member member);
    Optional<Account> findByMember(Member member);
    @Query(value = "SELECT nextval('account_num_seq')", nativeQuery = true)
    Long getNextAccountSequence();
}
