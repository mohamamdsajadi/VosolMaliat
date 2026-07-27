package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fake_restriction_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeRestrictionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "official_gazette_request_id", nullable = false, unique = true)
    private String officialGazetteRequestId;

    @Column(name = "execution_paper_id", nullable = false)
    private Long executionPaperId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "related_person_count", nullable = false)
    private Integer relatedPersonCount;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "sent_at")
    private String sentAt;

    @Column(name = "confirmed_at")
    private String confirmedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
