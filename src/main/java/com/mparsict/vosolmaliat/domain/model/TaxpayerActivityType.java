package com.mparsict.vosolmaliat.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "taxpayer_activity_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxpayerActivityType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_id")
    private Long taxpayerId;

    @Column(name = "activity_type")
    private String activityType;
}
