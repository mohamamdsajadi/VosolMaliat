package com.mparsict.vosolmaliat.domain.model.masterdata;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "paper_type", uniqueConstraints =
        @UniqueConstraint(name = "uk_paper_type_code", columnNames = "code"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperTypeDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 32)
    private String code;

    @Column(name = "paper_title", nullable = false, updatable = false)
    private String paperTitle;
}
