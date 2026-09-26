package com.mparsict.vosolmaliat.domain.model.execution;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
@Entity @Table(name="debt_information_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DebtInformationHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="debt_information_id",nullable=false)
    private DebtInformation debtInformation;
    @Embedded private SanimAmounts amounts;
    @Embedded private DebtFinancialDetails financialDetails;
    private LocalDateTime effectiveAt;
    private LocalDateTime archivedAt;
    private String inquiryRequestId;
}
