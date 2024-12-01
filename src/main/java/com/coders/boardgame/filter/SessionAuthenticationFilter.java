package com.coders.boardgame.filter;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 예외 경로 처리
        if (Arrays.stream(excludedPaths).anyMatch(requestURI::startsWith)) {
            log.info("Request URI '{}' is excluded from authentication.", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 세션 확인
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) == null) {
            log.warn("Unauthenticated request to '{}'. Returning 401.", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session is invalid or expired");
            return;
        }


        // SecurityContext 설정
        SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContextHolder.setContext(securityContext);

        // 세션에서 userId 가져오기
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            log.warn("요청 URI '{}'에서 세션에 userId가 없습니다. 401 응답을 반환합니다.", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "세션에 사용자 ID가 없습니다.");
            return;
        }

        log.info("Authenticated request to '{}'. Proceeding with filter chain.", requestURI);
        filterChain.doFilter(request, response);
    }
}
