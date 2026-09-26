package com.mparsict.vosolmaliat.domain.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="taxpayer_activity_type_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxpayerActivityTypeHistory extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private TaxpayerActivityType activity;
    private String activityTypeCode;
    private String activityTypeTitle;
}
