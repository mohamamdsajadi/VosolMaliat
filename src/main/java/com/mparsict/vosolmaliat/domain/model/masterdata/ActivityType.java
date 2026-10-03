package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "activity_type")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ActivityType extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "inta_code", nullable = false, unique = true) private String intaCode;
    @Column(nullable = false) private String title;
}
