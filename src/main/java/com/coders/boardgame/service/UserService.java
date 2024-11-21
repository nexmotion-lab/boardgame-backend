package com.coders.boardgame.service;

import com.coders.boardgame.dto.UserDto;
import com.coders.boardgame.entity.User;
import com.coders.boardgame.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final HttpSession session;


    public String signUp(UserDto userDto) {
        User user = new User(userDto);
        userRepository.save(user);

        // 유저 인증 후, 세션 생성
        authenticateUser(user);

        // 세션id 리턴
        return session.getId();
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, new ArrayList<>());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
