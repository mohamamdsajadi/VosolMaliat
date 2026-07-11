package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "debt_detection_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtDetectionDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_id")
    private Long executionPaperId;
}
