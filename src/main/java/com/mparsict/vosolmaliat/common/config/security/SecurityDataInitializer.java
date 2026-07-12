package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.Permission;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository;
import com.mparsict.vosolmaliat.domain.repository.security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityDataInitializer {

    private static final List<String> PERMISSIONS = List.of(
            "USER_PROFILE_CREATE", "USER_PROFILE_UPDATE", "USER_PROFILE_VIEW", "USER_PROFILE_ENABLE_DISABLE",
            "USER_PROFILE_ASSIGN_ORGANIZATION", "PERMISSION_VIEW", "PERMISSION_ASSIGN", "GROUP_PERMISSION_VIEW",
            "GROUP_PERMISSION_UPDATE", "TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE", "TASK_CLAIM", "TASK_UNCLAIM",
            "TASK_COMPLETE", "TASK_ASSIGN", "TASK_DELEGATE", "TASK_VIEW_ORG_SCOPE", "REPORT_VIEW", "AUDIT_VIEW"
    );

    private static final List<String> OPERATIONAL_TASK_PERMISSIONS = List.of(
            "TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE", "TASK_CLAIM", "TASK_UNCLAIM", "TASK_COMPLETE", "TASK_VIEW_ORG_SCOPE"
    );

    private static final List<String> MANAGER_TASK_PERMISSIONS = List.of(
            "TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE", "TASK_CLAIM", "TASK_UNCLAIM", "TASK_COMPLETE", "TASK_DELEGATE", "TASK_VIEW_ORG_SCOPE"
    );

    private final PermissionRepository permissionRepository;
    private final GroupPermissionRepository groupPermissionRepository;
    private final AppUserProfileRepository userProfileRepository;
    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorateRepository;
    private final TaxAffairsOfficeRepository taxAffairsOfficeRepository;
    private final TaxUnitRepository taxUnitRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.security.bootstrap-admin.keycloak-user-id:${ADMIN_KEYCLOAK_USER_ID:dev-admin-keycloak-user-id}}")
    private String bootstrapAdminKeycloakUserId;

    @Value("${app.security.bootstrap-admin.username:${ADMIN_USERNAME:admin}}")
    private String bootstrapAdminUsername;

    @Value("${app.security.bootstrap-admin.email:${ADMIN_EMAIL:admin@vosol-maliat.local}}")
    private String bootstrapAdminEmail;

    @Bean
    CommandLineRunner seedSecurityData() {
        return args -> transactionTemplate.executeWithoutResult(status -> {
            for (String code : PERMISSIONS) {
                permissionRepository.findByCode(code).orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code(code)
                        .description(code)
                        .enabled(true)
                        .build()));
            }
            for (String code : PERMISSIONS) {
                ensureGroupPermission("ADMIN", code);
            }
            for (String code : OPERATIONAL_TASK_PERMISSIONS) {
                ensureGroupPermission("TAX_UNIT_USER", code);
            }
            for (String role : List.of("GENERAL_OFFICE_HEAD", "TAX_OFFICE_HEAD", "TAX_UNIT_HEAD")) {
                for (String code : MANAGER_TASK_PERMISSIONS) {
                    ensureGroupPermission(role, code);
                }
            }
            for (String code : OPERATIONAL_TASK_PERMISSIONS) {
                ensureGroupPermission("TAX_EXPERT", code);
            }
            seedLocalAdminProfile();
            seedSampleOperationalUsers();
        });
    }

    private void ensureGroupPermission(String groupName, String permissionCode) {
        if (groupPermissionRepository.findByGroupNameAndPermissionCode(groupName, permissionCode).isPresent()) {
            return;
        }
        Permission permission = permissionRepository.findByCode(permissionCode).orElseThrow();
        groupPermissionRepository.save(GroupPermission.builder()
                .groupName(groupName)
                .permission(permission)
                .build());
    }

    private void seedLocalAdminProfile() {
        if (bootstrapAdminKeycloakUserId == null || bootstrapAdminKeycloakUserId.isBlank()
                || userProfileRepository.existsByKeycloakUserId(bootstrapAdminKeycloakUserId)
                || userProfileRepository.existsByUsername(bootstrapAdminUsername)
                || userProfileRepository.existsByEmail(bootstrapAdminEmail)) {
            return;
        }
        userProfileRepository.save(AppUserProfile.builder()
                .keycloakUserId(bootstrapAdminKeycloakUserId)
                .username(bootstrapAdminUsername)
                .email(bootstrapAdminEmail)
                .firstName("System")
                .lastName("Administrator")
                .businessRole(com.mparsict.vosolmaliat.domain.enums.BusinessRole.ADMIN)
                .enabledInBusinessSystem(true)
                .lockedInBusinessSystem(false)
                .build());
    }

    private void seedSampleOperationalUsers() {
        TaxUnit taxUnit = sampleTaxUnit();
        TaxAffairsOffice office = taxAffairsOfficeRepository.findById(taxUnit.getTaxAffairsOfficeId()).orElseThrow();
        GeneralDirectorateOfTaxAffairs general = generalDirectorateRepository
                .findById(office.getGeneralDirectorateOfTaxAffairsId())
                .orElseThrow();

        seedSampleUser(
                "2d7e1db3-7b99-4f5d-99eb-9de9e031dea8",
                "general_head",
                "general.head@vosol-maliat.local",
                "رئیس",
                "اداره کل",
                BusinessRole.GENERAL_OFFICE_HEAD,
                general.getId(),
                null,
                null
        );
        seedSampleUser(
                "eedab29f-d493-44a8-b71a-67a5cb721154",
                "tax_office_head",
                "tax.office.head@vosol-maliat.local",
                "رئیس",
                "اداره مالیات",
                BusinessRole.TAX_OFFICE_HEAD,
                general.getId(),
                office.getId(),
                null
        );
        seedSampleUser(
                "aae48551-addf-478d-9d68-3a1f2a947404",
                "tax_unit_head",
                "tax.unit.head@vosol-maliat.local",
                "رئیس",
                "واحد مالیاتی",
                BusinessRole.TAX_UNIT_HEAD,
                general.getId(),
                office.getId(),
                taxUnit
        );
        seedSampleUser(
                "8c7b79dc-768c-44d6-8f36-fec5e600358b",
                "tax_expert",
                "tax.expert@vosol-maliat.local",
                "کارشناس",
                "مالیاتی",
                BusinessRole.TAX_EXPERT,
                general.getId(),
                office.getId(),
                taxUnit
        );
    }

    private TaxUnit sampleTaxUnit() {
        if (taxUnitRepository.count() > 0) {
            return taxUnitRepository.findAll().getFirst();
        }
        GeneralDirectorateOfTaxAffairs general = generalDirectorateRepository.save(GeneralDirectorateOfTaxAffairs.builder()
                .generalDirectorateName("اداره کل نمونه امور مالیاتی")
                .postalCode("1111111111")
                .address("تهران، ساختمان نمونه اداره کل")
                .phone("02111111111")
                .build());
        TaxAffairsOffice office = taxAffairsOfficeRepository.save(TaxAffairsOffice.builder()
                .generalDirectorateOfTaxAffairsId(general.getId())
                .officeName("اداره مالیات نمونه")
                .postalCode("2222222222")
                .address("تهران، ساختمان نمونه اداره مالیات")
                .phone("02122222222")
                .build());
        return taxUnitRepository.save(TaxUnit.builder()
                .taxAffairsOfficeId(office.getId())
                .taxUnitName("واحد مالیاتی نمونه")
                .postalCode("3333333333")
                .address("تهران، ساختمان نمونه واحد مالیاتی")
                .phone("02133333333")
                .build());
    }

    private void seedSampleUser(
            String keycloakUserId,
            String username,
            String email,
            String firstName,
            String lastName,
            BusinessRole businessRole,
            Long generalDirectorateId,
            Long taxAffairsOfficeId,
            TaxUnit taxUnit) {
        if (userProfileRepository.existsByKeycloakUserId(keycloakUserId)
                || userProfileRepository.existsByUsername(username)
                || userProfileRepository.existsByEmail(email)) {
            return;
        }
        userProfileRepository.save(AppUserProfile.builder()
                .keycloakUserId(keycloakUserId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber("09120000000")
                .businessRole(businessRole)
                .generalDirectorateId(generalDirectorateId)
                .taxAffairsOfficeId(taxAffairsOfficeId)
                .taxUnit(taxUnit)
                .enabledInBusinessSystem(true)
                .lockedInBusinessSystem(false)
                .build());
    }
}
