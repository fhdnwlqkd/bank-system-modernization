package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("ADMIN")
public class Admin extends Member {
    private String department;

    // 부서 이동
    public void updateAdmin(String department) {
        if (department != null) this.department = department;
    }
    public static Admin create(
            String loginId,
            String password,
            String name,
            String contact,
            String department
    ) {
        return Admin.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .contact(contact)
                .department(department)
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
