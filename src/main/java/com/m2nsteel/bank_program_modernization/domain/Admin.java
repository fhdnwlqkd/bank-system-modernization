package com.m2nsteel.bank_program_modernization.domain;

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
@DiscriminatorValue("ADMIN")
public class Admin extends Member {
    private String department;

    // 부서 이동
    public void changeDepartment(String newDepartment) {
        this.department = newDepartment;
    }
}
