package com.escrowflow.repository;

import com.escrowflow.domain.UserWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {

    long countByUser_Id(Long userId);

    @Query("""
            SELECT w FROM UserWarning w
            JOIN FETCH w.issuedBy
            WHERE w.user.id = :userId
            ORDER BY w.createdAt DESC
            """)
    List<UserWarning> findByUserIdWithIssuer(@Param("userId") Long userId);
}
