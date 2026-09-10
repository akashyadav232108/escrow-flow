package com.escrowflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "project_exit_settlements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_exit_settlements_exit_milestone",
                columnNames = {"project_exit_id", "milestone_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectExitSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_exit_id", nullable = false)
    private ProjectExit projectExit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @Column(name = "hold_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal holdAmount;

    @Column(name = "freelancer_amount", precision = 19, scale = 4)
    private BigDecimal freelancerAmount;

    @Column(name = "client_refund_amount", precision = 19, scale = 4)
    private BigDecimal clientRefundAmount;
}
