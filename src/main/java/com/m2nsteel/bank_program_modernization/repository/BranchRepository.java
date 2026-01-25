package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByName(String name);
    @Query(value = "SELECT nextval('branch_num_seq')", nativeQuery = true)
    Long getNextBranchSequence();
}
