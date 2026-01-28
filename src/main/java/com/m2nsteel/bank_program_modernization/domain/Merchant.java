package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    public void updateMerchant(String shopName, String category) {
        if (shopName != null) this.shopName = shopName;
        if (category != null) this.category = category;
    }

    public static Merchant create(
            String loginId,
            String password,
            String name,
            String contact,
            String businessNumber,
            String shopName,
            String category
    ) {
        return Merchant.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .contact(contact)
                .businessNumber(businessNumber)
                .shopName(shopName)
                .category(category)
                .role(MemberRole.MERCHANT)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}