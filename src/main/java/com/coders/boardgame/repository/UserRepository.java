package com.coders.boardgame.repository;

import com.coders.boardgame.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
