package com.coders.boardgame.domain.user.repository;

import com.coders.boardgame.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
