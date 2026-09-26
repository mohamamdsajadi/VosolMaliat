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
    @Version private Long version;
    @OneToMany(mappedBy="activity", cascade=CascadeType.ALL)
    @Builder.Default private java.util.List<TaxpayerActivityTypeHistory> history = new java.util.ArrayList<>();
    public void updateTitle(String title) {
        if (java.util.Objects.equals(activityTypeTitle,title)) return;
        if (id != null) history.add(TaxpayerActivityTypeHistory.builder().activity(this)
            .activityTypeCode(activityTypeCode).activityTypeTitle(activityTypeTitle).build());
        activityTypeTitle=title;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_type_code")
    private String activityTypeCode;

    @Column(name = "activity_type_title")
    private String activityTypeTitle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxpayer_id", unique=true, nullable=false)
    private Taxpayer taxpayer;

    public Long getTaxpayerId() {
        return taxpayer == null ? null : taxpayer.getId();
    }

    public void setTaxpayerId(Long taxpayerId) {
        this.taxpayer = taxpayerId == null ? null : Taxpayer.builder().id(taxpayerId).build();
    }
}
