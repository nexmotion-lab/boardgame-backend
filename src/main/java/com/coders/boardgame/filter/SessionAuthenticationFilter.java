package com.coders.boardgame.filter;

import com.coders.boardgame.exception.CustomAuthenticationEntryPoint;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final String[] excludedPaths;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 예외 경로 처리
        if (Arrays.stream(excludedPaths).anyMatch(requestURI::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }


        // 세션 확인
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) == null) {
            log.warn(" 인증되지 않은 요청 '{}'. Returning 401.", requestURI);
            // 인증 실패 시 SecurtiyContext 지우고
            SecurityContextHolder.clearContext();

            // 시큐리티 표준 AuthenticationException 구현체로 하나 익명 클래스를 만듬
            AuthenticationException authException = new AuthenticationException("세션이 만료되었거나 존재하지 않습니다.") { };

            // EntryPoint를 직접 호출 -> 여기서 401 + 바디 생성
            customAuthenticationEntryPoint.commence(request, response, authException);
            return;
        }


        // SecurityContext 설정
        SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContextHolder.setContext(securityContext);

        // 세션에서 userId 가져오기
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            log.warn("요청 URI '{}'에서 세션에 userId가 없습니다. 401 응답을 반환합니다.", requestURI);
            SecurityContextHolder.clearContext();
            AuthenticationException authException = new AuthenticationException("사용자 ID가 없습니다.") { };
            customAuthenticationEntryPoint.commence(request, response, authException);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
