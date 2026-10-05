package com.mparsict.vosolmaliat.domain.model;

import com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name = "taxpayer_activity_type")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxpayerActivityType extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "taxpayer_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore private Taxpayer taxpayer;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "activity_type_id", nullable = false)
    private ActivityType activityType;
    @Version private Long version;
    @OneToMany(mappedBy = "activity", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default @com.fasterxml.jackson.annotation.JsonIgnore
    private List<TaxpayerActivityTypeHistory> history = new ArrayList<>();

    public void changeActivity(ActivityType next) {
        Objects.requireNonNull(next, "نوع فعالیت الزامی است");
        if (activityType != null && Objects.equals(activityType.getId(), next.getId())) return;
        if (id != null) history.add(TaxpayerActivityTypeHistory.from(this));
        activityType = next;
    }
    public String getActivityTypeCode() { return activityType == null ? null : activityType.getIntaCode(); }
    public String getActivityTypeTitle() { return activityType == null ? null : activityType.getTitle(); }
    public Long getTaxpayerId() { return taxpayer == null ? null : taxpayer.getId(); }
}
