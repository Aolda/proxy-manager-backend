package com.aolda.itda.repository.user;

import com.aolda.itda.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeystoneId(String keystoneId);
    Optional<User> findByKeystoneUsername(String keystoneUsername);
}
