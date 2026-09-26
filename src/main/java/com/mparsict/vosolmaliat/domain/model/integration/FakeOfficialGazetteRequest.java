package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fake_official_gazette_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeOfficialGazetteRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "execution_paper_id", nullable = false)
    private Long executionPaperId;

    @Column(name = "legal_person_national_id", nullable = false)
    private String legalPersonNationalId;

    @Column(name = "debt_paper_date", nullable = false)
    private String debtPaperDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sent_at")
    private String sentAt;

    @Column(name = "responded_at")
    private String respondedAt;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
