package com.coders.boardgame.service;

import com.coders.boardgame.dto.UserDto;
import com.coders.boardgame.entity.User;
import com.coders.boardgame.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void signUp(UserDto userDto) {
        User user = new User(userDto);

        userRepository.save(user);
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, new ArrayList<>());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
