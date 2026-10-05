package com.mparsict.vosolmaliat.domain.model;
import com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "taxpayer_activity_type_history") @org.hibernate.annotations.Immutable
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxpayerActivityTypeHistory extends VersionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false) private TaxpayerActivityType activity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "taxpayer_id", nullable = false) private Taxpayer taxpayer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "activity_type_id", nullable = false) private ActivityType activityType;
    public static TaxpayerActivityTypeHistory from(TaxpayerActivityType source) {
        var row = TaxpayerActivityTypeHistory.builder().activity(source).taxpayer(source.getTaxpayer()).activityType(source.getActivityType()).build();
        row.captureAudit(source);
        return row;
    }
}
