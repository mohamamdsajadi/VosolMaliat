package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "general_directorate_of_tax_affairs_history", indexes =
        @Index(name = "idx_general_directorate_history_entity", columnList = "general_directorate_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralDirectorateOfTaxAffairsHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "general_directorate_id", nullable = false, updatable = false)
    private Long generalDirectorateId;

    @Column(name = "general_directorate_name", nullable = false, updatable = false)
    private String generalDirectorateName;

    @Column(name = "postal_code", updatable = false)
    private String postalCode;

    @Column(name = "address", updatable = false)
    private String address;

    @Column(name = "phone", updatable = false)
    private String phone;

    @Column(name = "change_type", nullable = false, updatable = false)
    private String changeType;

    public static GeneralDirectorateOfTaxAffairsHistory from(
            GeneralDirectorateOfTaxAffairs source, String changeType) {
        return GeneralDirectorateOfTaxAffairsHistory.builder()
                .generalDirectorateId(source.getId())
                .generalDirectorateName(source.getGeneralDirectorateName())
                .postalCode(source.getPostalCode()).address(source.getAddress()).phone(source.getPhone())
                .changeType(changeType).build();
    }
}
