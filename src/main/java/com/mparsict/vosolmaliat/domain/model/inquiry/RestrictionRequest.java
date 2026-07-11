package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restriction_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestrictionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finalized_debt_paper_notification_id")
    private String finalizedDebtPaperNotificationId;

    @Column(name = "natural_person_id")
    private Long naturalPersonId;

    @Column(name = "legal_person_id")
    private Long legalPersonId;

    @Column(name = "result")
    private Integer result;
}
