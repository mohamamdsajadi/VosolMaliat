package com.mparsict.vosolmaliat.domain.model.masterdata;
import com.mparsict.vosolmaliat.domain.model.VersionHistory;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "activity_type_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ActivityTypeHistory extends VersionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "activity_type_id", nullable = false) private Long activityTypeId;
    @Column(name = "inta_code", nullable = false) private String intaCode;
    @Column(nullable = false) private String title;
    public static ActivityTypeHistory from(ActivityType source) {
        var row = ActivityTypeHistory.builder().activityTypeId(source.getId()).intaCode(source.getIntaCode()).title(source.getTitle()).build();
        row.captureAudit(source);
        return row;
    }
}
