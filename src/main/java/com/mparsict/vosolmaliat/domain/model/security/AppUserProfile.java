package com.mparsict.vosolmaliat.domain.model.security;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_profile_keycloak_user_id", columnNames = "keycloak_user_id"),
        @UniqueConstraint(name = "uk_app_user_profile_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_app_user_profile_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Column(nullable = false)
    private String username;

    @Column
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_unit_id")
    private TaxUnit taxUnit;

    @Builder.Default
    @Column(name = "enabled_in_business_system", nullable = false)
    private Boolean enabledInBusinessSystem = true;

    @Builder.Default
    @Column(name = "locked_in_business_system", nullable = false)
    private Boolean lockedInBusinessSystem = false;
}
