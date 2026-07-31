package com.escrowflow.repository;

import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface EscrowHoldRepository extends JpaRepository<EscrowHold, Long> {

    Optional<EscrowHold> findByMilestoneId(Long milestoneId);

    @Query("""
            SELECT COALESCE(SUM(h.amount), 0)
            FROM EscrowHold h
            WHERE h.status = :status
            """)
    BigDecimal sumAmountByStatus(@Param("status") EscrowHoldStatus status);

    @Query("""
            SELECT COUNT(h) FROM EscrowHold h
            JOIN h.milestone m
            JOIN m.project p
            WHERE h.status = com.escrowflow.domain.enums.EscrowHoldStatus.HELD
              AND p.client.id = :userId
            """)
    long countHeldEscrowForClient(@Param("userId") Long userId);
}
