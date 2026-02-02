package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
@Table(name = "members")
public class Member extends BaseEntity {
    private String name;
    private String contact;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    public void updateInfo(String name, String contact, @Nullable String encodedPassword) {
        if (name != null) this.name = name;
        if (contact != null) this.contact = contact;
        if (encodedPassword != null) this.password = encodedPassword;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public static Member create(String loginId, String password, String name, String contact) {
        return Member.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .contact(contact)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
