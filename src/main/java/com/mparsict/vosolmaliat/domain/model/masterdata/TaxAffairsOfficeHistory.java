package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "tax_affairs_office_history", indexes =
        @Index(name = "idx_tax_affairs_office_history_entity", columnList = "tax_affairs_office_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxAffairsOfficeHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_affairs_office_id", nullable = false, updatable = false)
    private Long taxAffairsOfficeId;

    @Column(name = "general_directorate_id", nullable = false, updatable = false)
    private Long generalDirectorateId;

    @Column(name = "office_name", nullable = false, updatable = false)
    private String officeName;

    @Column(name = "postal_code", updatable = false)
    private String postalCode;

    @Column(name = "address", updatable = false)
    private String address;

    @Column(name = "phone", updatable = false)
    private String phone;

    @Column(name = "change_type", nullable = false, updatable = false)
    private String changeType;

    public static TaxAffairsOfficeHistory from(TaxAffairsOffice source, String changeType) {
        return TaxAffairsOfficeHistory.builder()
                .taxAffairsOfficeId(source.getId())
                .generalDirectorateId(source.getGeneralDirectorateOfTaxAffairsId())
                .officeName(source.getOfficeName()).postalCode(source.getPostalCode())
                .address(source.getAddress()).phone(source.getPhone()).changeType(changeType).build();
    }
}
