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
     * user생성
     *
     * @param userDto
     * @param request
     * @return sessionId 반환
     */
    public String signUp(UserDto userDto, HttpServletRequest request) {

        // 1. 사용자 생성 및 저장
        User user = new User(userDto);
        userRepository.save(user);

        // 3. HttpSession과 SecurityContext 동기화
        return synchronizeSessionAndSecurityContext(request, user);
    }

    /**
     * 유저 인증 객체 생성
     * @param user
     */
    private void authenticateUser(User user) {
        // Spring Security 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, new ArrayList<>());

        // SecurityContextHolder에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     *
     *
     * @param request
     * @param user
     * @return sessionId 반환
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
