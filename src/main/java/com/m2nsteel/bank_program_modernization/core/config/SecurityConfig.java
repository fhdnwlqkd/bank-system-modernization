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
                        // 1. 누구나 접근 허용 (회원가입, 로그인, Swagger)
                        .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchants").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admins").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // 2. 가맹점 전용 API: MERCHANT 또는 ADMIN만 접근 가능
                        .requestMatchers("/api/merchants/me/**").hasAnyRole("MERCHANT", "ADMIN")

                        // 3. 관리자 전용 API: 오직 ADMIN만 접근 가능
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")

                        // 4. 그 외 모든 요청은 인증된 사용자라면 누구나 가능
                        .anyRequest().authenticated()
                )

                // 4. JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
        빈 등록 차단
     */
    @Bean
    @NullMarked
    public FilterRegistrationBean<JwtAuthenticationFilter> registration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
