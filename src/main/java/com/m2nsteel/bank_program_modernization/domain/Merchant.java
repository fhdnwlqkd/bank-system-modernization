package com.m2nsteel.bank_program_modernization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("MERCHANT")
public class Merchant extends Member {

    @Column(unique = true)
    private String businessNumber;

    private String shopName;
    private String category;

    public void updateMerchantInfo(String shopName, String category) {
        this.shopName = shopName;
        this.category = category;
    }
}