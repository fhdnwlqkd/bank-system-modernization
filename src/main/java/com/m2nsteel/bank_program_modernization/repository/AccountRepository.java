package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAllByMember(Member member);
    Optional<Account> findByExternalId(String externalId);
    boolean existsByMember(Member member);
    Optional<Account> findByMember(Member member);
    @Query(value = "SELECT nextval('account_num_seq')", nativeQuery = true)
    Long getNextAccountSequence();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Account a " +
            "SET a.balance = a.balance + :amount " +
            "WHERE a.id = :id " +
            "AND (:amount >= 0 OR a.balance + :amount >= 0)") // 출금 시 잔액 체크
    int updateBalance(@Param("id") Long id, @Param("amount") Long amount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select a
           from Account a
           where a.accountNumber = :accountNumber
             and a.status = 'ACTIVE'
           """)
    Optional<Account> findActiveByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
