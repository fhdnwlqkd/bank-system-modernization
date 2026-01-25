package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class BranchServiceTest {
    @Autowired BranchService branchService;
    @Test
    @DisplayName("지점 생성 성공")
    void createBranch_success() {
        // 1. Given: 테스트를 위한 준비
        String branchName = "Downtown Branch";
        String address = "123 Main St, Cityville";
        String contact = "555-1234";
        BranchCreateRequest request = new BranchCreateRequest(
                branchName, address, contact
        );

        // 2. When & Then: 결과 검증
        assertThat(branchService.createBranch(request).name()).isEqualTo(branchName);
    }

    @Test
    @DisplayName("지점 이름 중복 시 예외 발생")
    void createBranch_fail_duplicateName() {
        // given: 동일한 지점 이름으로 두 번 생성 요청
        String branchName = "Uptown Branch";
        BranchCreateRequest request1 = new BranchCreateRequest(
                branchName, "456 Elm St, Cityville", "555-5678"
        );
        BranchCreateRequest request2 = new BranchCreateRequest(
                branchName, "789 Oak St, Cityville", "555-9012"
        );

        // when: 첫 번째 생성은 성공
        branchService.createBranch(request1);

        // then: 두 번째 생성 시 예외 발생
        assertThatThrownBy(() -> branchService.createBranch(request2))
                .isInstanceOf(BusinessException.class);
    }

}