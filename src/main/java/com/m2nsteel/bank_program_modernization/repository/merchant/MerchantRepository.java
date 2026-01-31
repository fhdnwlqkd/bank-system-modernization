package com.m2nsteel.bank_program_modernization.repository.merchant;

import com.m2nsteel.bank_program_modernization.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long>, MerchantRepositoryCustom {
    Optional<Merchant> findByBusinessNumber(String businessNumber);
    Optional<Merchant> findByExternalId(String externalId);
}
