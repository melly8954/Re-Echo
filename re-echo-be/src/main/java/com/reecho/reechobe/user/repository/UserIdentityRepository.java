package com.reecho.reechobe.user.repository;

import com.reecho.reechobe.user.domain.OAuthProvider;
import com.reecho.reechobe.user.domain.UserIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 소셜 로그인 공급자 계정 연결 정보의 영속성 접근을 담당한다.
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );
}
