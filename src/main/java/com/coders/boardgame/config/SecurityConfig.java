package com.coders.boardgame.config;

import com.coders.boardgame.exception.CustomAuthenticationEntryPoint;
import com.coders.boardgame.filter.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint; // 주입받은 빈 사용

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


        // 예외 경로를 배열로 정의
        String[] permitted = {
                "/api/users","/api/users/session", "/api/schools"
        };

        http
//                .requiresChannel(channel -> channel
//                        .anyRequest().requiresSecure() // 모든 요청을 HTTPS로 강제 리다이렉트
//                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정
                .csrf(AbstractHttpConfigurer::disable)                              //  CSRF 보호 비활성화 (API 사용 시 보통 비활성화)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(permitted).permitAll()   // 회원가입 및 학교 정보 검색 경로는 인증없이 접근 가능
                        .anyRequest().authenticated()                   // 그 외 모든 요청은 인증 필요
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)       // 세션 정책 설정
                )
                .addFilterBefore(new SessionAuthenticationFilter(permitted), UsernamePasswordAuthenticationFilter.class); // 세션 인증 필터

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of(frontendUrl, "https://localhost"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}