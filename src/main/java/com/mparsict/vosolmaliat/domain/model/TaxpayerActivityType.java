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

    @Column(name = "activity_type")
    private String activityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxpayer_id")
    private Taxpayer taxpayer;

    public Long getTaxpayerId() {
        return taxpayer == null ? null : taxpayer.getId();
    }

    public void setTaxpayerId(Long taxpayerId) {
        this.taxpayer = taxpayerId == null ? null : Taxpayer.builder().id(taxpayerId).build();
    }
}
