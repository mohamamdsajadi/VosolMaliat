package com.mparsict.vosolmaliat.domain.model.security;

import com.mparsict.vosolmaliat.domain.enums.security.OrganizationUnitType;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization_unit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_organization_unit_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationUnitType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private OrganizationUnit parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    private List<OrganizationUnit> children = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
