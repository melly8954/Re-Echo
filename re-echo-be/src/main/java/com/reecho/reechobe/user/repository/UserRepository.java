package com.reecho.reechobe.user.repository;

import com.reecho.reechobe.user.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 사용자 계정 영속성 접근을 담당한다.
public interface UserRepository extends JpaRepository<User, UUID> {
}
