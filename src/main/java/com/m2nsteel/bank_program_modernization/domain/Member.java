package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name="role")
@Table(name = "members")
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contact;

    @Column(unique = true, nullable = false)
    private String memberNumber;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;
}
