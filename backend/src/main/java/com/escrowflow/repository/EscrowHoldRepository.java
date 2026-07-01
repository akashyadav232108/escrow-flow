package com.escrowflow.repository;

import com.escrowflow.domain.EscrowHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscrowHoldRepository extends JpaRepository<EscrowHold, Long> {

    Optional<EscrowHold> findByMilestoneId(Long milestoneId);
}
