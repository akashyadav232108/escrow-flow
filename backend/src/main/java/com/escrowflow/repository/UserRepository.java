package com.escrowflow.repository;

import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.AccountStatus;
import com.escrowflow.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    long countByAccountStatus(AccountStatus accountStatus);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.createdBy
            WHERE u.role IN :roles
            ORDER BY u.createdAt DESC
            """)
    List<User> findByRoleInWithCreatedBy(@Param("roles") Collection<UserRole> roles);

    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND (:status IS NULL OR u.accountStatus = :status)
            ORDER BY u.createdAt DESC
            """)
    List<User> findMarketplaceUsers(
            @Param("roles") Collection<UserRole> roles,
            @Param("status") AccountStatus status);
}
