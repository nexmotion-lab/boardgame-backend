package com.coders.boardgame.domain.user.service;

import com.coders.boardgame.domain.user.dto.UserDto;
import com.coders.boardgame.domain.user.entity.User;
import com.coders.boardgame.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 회원가입 처리: 중복 요청 방지와 함께 사용자 저장 및 세션/보안 동기화를 수행합니다.
     *
     * @param userDto 사용자 정보 DTO
     * @param request HTTP 요청 (세션 관리에 사용)
     * @return 생성되었거나 기존의 세션 ID 반환
     * @throws IllegalStateException 중복 요청 시 예외 발생
     */
    public String signUp(UserDto userDto, HttpServletRequest request) {

        // HttpSession 객체를 가져오거나 생성한다.
        HttpSession session = request.getSession();

        // 중복 가입 요청 방지를 위해 세션 플래그 확인
        if (Boolean.TRUE.equals(session.getAttribute("SIGNUP_IN_PROGRESS"))) {
            throw new IllegalStateException("회원가입 요청이 이미 진행 중입니다");
        }

        // 회원가입 처리 시작 표시
        session.setAttribute("SIGNUP_IN_PROGRESS", true);

        try{
            // 1. 사용자 생성 및 저장
            User user = new User(userDto);
            userRepository.save(user);

            // 3. HttpSession과 SecurityContext 동기화
            return synchronizeSessionAndSecurityContext(request, user);
        } finally {
            // 요청 처리 후 플래그 제거
            session.removeAttribute("SIGNUP_IN_PROGRESS");
        }

    }

    /**
     * 스프링 시큐리티 인증 객체를 생성하여 SecurityContext에 저장합니다.
     *
     * @param user 가입된 사용자 객체
     */
    private void authenticateUser(User user) {
        // Spring Security 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, new ArrayList<>());

        // SecurityContextHolder에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * HttpSession을 생성(또는 기존 세션 사용)하고, 사용자 식별값과 SecurityContext를 세션에 저장합니다.
     *
     * @param request HTTP 요청
     * @param user 가입된 사용자 객체
     * @return 세션 ID
     */
    private String synchronizeSessionAndSecurityContext(HttpServletRequest request, User user) {

        // 1. Spring Security Context 설정
        authenticateUser(user);

        // 2. HttpSession 생성 및 userId 저장
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());

        // 3. SecurityContext와 HttpSession 동기화
        SecurityContext securityContext = SecurityContextHolder.getContext();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return session.getId(); // 생성된 세션 ID 반환
    }

}
