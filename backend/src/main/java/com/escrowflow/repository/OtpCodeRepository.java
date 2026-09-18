package com.escrowflow.repository;

import com.escrowflow.domain.OtpCode;
import com.escrowflow.domain.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.email = :email AND o.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
