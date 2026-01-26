package com.m2nsteel.bank_program_modernization.domain;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@DiscriminatorValue("MERCHANT")
@Table(name = "merchant_members")
public class MerchantMember extends Member {
    private String businessRegistrationNumber;
    private String merchantCategory;
}
