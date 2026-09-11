package com.tilink.global.security;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 *
 * <p>JWT 기반의 stateless 인증이므로 세션·CSRF·폼 로그인·HTTP Basic 을 모두 끄고,
 * 직접 만든 {@link JwtAuthenticationFilter} 로 인증 정보를 채운다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    // 토큰 없이 접근할 수 있는 엔드포인트. 여기 없는 경로는 모두 인증이 필요하다.
    private static final String[] PUBLIC_GET_ENDPOINTS = {"/actuator/health"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandlers securityExceptionHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 토큰 인증이라 브라우저 세션 쿠키를 쓰지 않는다. CSRF 공격 경로 자체가 없어 비활성화한다.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 에러 응답(404 등)을 만드는 내부 ERROR 디스패치는 인가 대상에서 뺀다.
                        // stateless 라 원래 요청이 끝나면 SecurityContext 가 비워지는데, 그 상태로
                        // /error 가 다시 인가를 거치면 익명 요청으로 보여 404 가 401 로 바뀐다.
                        // 그러면 클라이언트는 토큰이 만료된 줄 알고 로그아웃해 버린다.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(securityExceptionHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(securityExceptionHandlers.accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 비밀번호 해싱. BCrypt 는 해시에 salt 와 강도를 함께 담으므로 별도 저장이 필요 없다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
