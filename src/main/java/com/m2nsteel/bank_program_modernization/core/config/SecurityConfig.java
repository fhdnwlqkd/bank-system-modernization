package com.m2nsteel.bank_program_modernization.core.config;

import com.m2nsteel.bank_program_modernization.core.security.JwtAuthenticationFilter;
import com.m2nsteel.bank_program_modernization.core.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 세션 미사용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입과 로그인은 누구에게나 허용
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()    // 일반 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/merchants").permitAll()  // 가맹점 가입
                        .requestMatchers(HttpMethod.POST, "/api/auth/tokens").permitAll() // 로그인(토큰 생성)

                        // 2. 관리자(ADMIN)만 접근 가능함
                        .requestMatchers(HttpMethod.POST, "/api/branches/**").hasRole("ADMIN")
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 4. JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
        빈 등록 차단 (수동 등록했기 떄문)
     */
    @Bean
    @NullMarked
    public FilterRegistrationBean<JwtAuthenticationFilter> registration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
