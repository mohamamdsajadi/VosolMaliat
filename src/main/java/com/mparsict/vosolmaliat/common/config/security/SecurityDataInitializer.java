package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.Permission;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
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
            "TASK_COMPLETE", "TASK_ASSIGN", "TASK_DELEGATE", "TASK_VIEW_ORG_SCOPE", "EXECUTION_PAPER_DETAIL_VIEW",
            "EXECUTION_PAPER_TRACE_VIEW", "REPORT_VIEW", "AUDIT_VIEW"
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
    private final TransactionTemplate transactionTemplate;
    private final KeycloakAdminBootstrapService keycloakAdminBootstrapService;

    @Value("${app.security.bootstrap-admin.keycloak-user-id:${ADMIN_KEYCLOAK_USER_ID:dev-admin-keycloak-user-id}}")
    private String bootstrapAdminKeycloakUserId;

    @Value("${app.security.bootstrap-admin.username:${ADMIN_USERNAME:admin}}")
    private String bootstrapAdminUsername;

    @Value("${app.security.bootstrap-admin.email:${ADMIN_EMAIL:admin@vosol-maliat.local}}")
    private String bootstrapAdminEmail;

    @Bean
    CommandLineRunner seedSecurityData() {
        return args -> initializeSecurityData();
    }

    public void initializeSecurityData() {
        transactionTemplate.executeWithoutResult(status -> {
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
            for (String role : List.of("GENERAL_OFFICE_HEAD", "TAX_OFFICE_HEAD")) {
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
        List<ProvinceSample> samples = List.of(
                new ProvinceSample("tehran", "تهران", "تهرانی", "021"),
                new ProvinceSample("isfahan", "اصفهان", "اصفهانی", "031"),
                new ProvinceSample("fars", "فارس", "شیرازی", "071"),
                new ProvinceSample("khorasan", "خراسان رضوی", "مشهدی", "051"),
                new ProvinceSample("azarbayjan", "آذربایجان شرقی", "تبریزی", "041")
        );
        samples.forEach(this::seedProvinceOperationalUsers);
    }

    private void seedProvinceOperationalUsers(ProvinceSample sample) {
        GeneralDirectorateOfTaxAffairs general = findOrCreateGeneralDirectorate(sample);
        TaxAffairsOffice office = findOrCreateTaxAffairsOffice(sample, general);
        seedSampleUser(
                "sample-" + sample.slug() + "-general-office-head",
                sample.slug() + "_general_office_head",
                sample.slug() + ".general.office.head@vosol-maliat.local",
                "مدیرکل",
                sample.lastName(),
                BusinessRole.GENERAL_OFFICE_HEAD,
                general.getId(),
                null
        );
        seedSampleUser(
                "sample-" + sample.slug() + "-tax-office-head",
                sample.slug() + "_tax_office_head",
                sample.slug() + ".tax.office.head@vosol-maliat.local",
                "رئیس اداره",
                sample.lastName(),
                BusinessRole.TAX_OFFICE_HEAD,
                general.getId(),
                office.getId()
        );
        seedSampleUser(
                "sample-" + sample.slug() + "-tax-expert",
                sample.slug() + "_tax_expert",
                sample.slug() + ".tax.expert@vosol-maliat.local",
                "کارشناس",
                sample.lastName(),
                BusinessRole.TAX_EXPERT,
                general.getId(),
                office.getId()
        );
    }

    private GeneralDirectorateOfTaxAffairs findOrCreateGeneralDirectorate(ProvinceSample sample) {
        String name = "اداره کل امور مالیاتی استان " + sample.provinceName();
        return generalDirectorateRepository.findAll().stream()
                .filter(item -> name.equals(item.getGeneralDirectorateName()))
                .findFirst()
                .orElseGet(() -> generalDirectorateRepository.save(GeneralDirectorateOfTaxAffairs.builder()
                        .generalDirectorateName(name)
                        .postalCode(sample.postalCode("100"))
                        .address("مرکز استان " + sample.provinceName() + "، ساختمان اداره کل امور مالیاتی")
                        .phone(sample.phonePrefix() + "0000100")
                        .build()));
    }

    private TaxAffairsOffice findOrCreateTaxAffairsOffice(ProvinceSample sample, GeneralDirectorateOfTaxAffairs general) {
        String name = "اداره امور مالیاتی مرکز " + sample.provinceName();
        return taxAffairsOfficeRepository.findByGeneralDirectorateOfTaxAffairsId(general.getId()).stream()
                .filter(item -> name.equals(item.getOfficeName()))
                .findFirst()
                .orElseGet(() -> taxAffairsOfficeRepository.save(TaxAffairsOffice.builder()
                        .generalDirectorateOfTaxAffairs(general)
                        .officeName(name)
                        .postalCode(sample.postalCode("200"))
                        .address("مرکز استان " + sample.provinceName() + "، اداره امور مالیاتی مرکزی")
                        .phone(sample.phonePrefix() + "0000200")
                        .build()));
    }

    private void seedSampleUser(
            String keycloakUserId,
            String username,
            String email,
            String firstName,
            String lastName,
            BusinessRole businessRole,
            Long generalDirectorateId,
            Long taxAffairsOfficeId) {
        String effectiveKeycloakUserId = keycloakAdminBootstrapService
                .ensureUser(username, email, firstName, lastName, businessRole)
                .orElse(keycloakUserId);

        AppUserProfile existingByUsername = userProfileRepository.findByUsername(username).orElse(null);
        if (existingByUsername != null) {
            updateSampleUser(existingByUsername, effectiveKeycloakUserId, email, firstName, lastName,
                    businessRole, generalDirectorateId, taxAffairsOfficeId);
            return;
        }

        if (userProfileRepository.existsByKeycloakUserId(effectiveKeycloakUserId)
                || userProfileRepository.existsByEmail(email)) {
            return;
        }

        userProfileRepository.save(AppUserProfile.builder()
                .keycloakUserId(effectiveKeycloakUserId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber("09120000000")
                .businessRole(businessRole)
                .generalDirectorate(generalDirectorateId == null ? null : GeneralDirectorateOfTaxAffairs.builder().id(generalDirectorateId).build())
                .taxAffairsOffice(taxAffairsOfficeId == null ? null : TaxAffairsOffice.builder().id(taxAffairsOfficeId).build())
                .enabledInBusinessSystem(true)
                .lockedInBusinessSystem(false)
                .build());
    }

    private void updateSampleUser(
            AppUserProfile profile,
            String keycloakUserId,
            String email,
            String firstName,
            String lastName,
            BusinessRole businessRole,
            Long generalDirectorateId,
            Long taxAffairsOfficeId) {
        userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .filter(other -> !other.getId().equals(profile.getId()))
                .ifPresent(other -> {
                    throw new IllegalStateException("Keycloak user id already belongs to another profile: " + keycloakUserId);
                });
        profile.setKeycloakUserId(keycloakUserId);
        profile.setEmail(email);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhoneNumber("09120000000");
        profile.setBusinessRole(businessRole);
        profile.setGeneralDirectorate(generalDirectorateId == null ? null : GeneralDirectorateOfTaxAffairs.builder().id(generalDirectorateId).build());
        profile.setTaxAffairsOffice(taxAffairsOfficeId == null ? null : TaxAffairsOffice.builder().id(taxAffairsOfficeId).build());
        profile.setEnabledInBusinessSystem(true);
        profile.setLockedInBusinessSystem(false);
        userProfileRepository.save(profile);
    }

    private record ProvinceSample(String slug, String provinceName, String lastName, String phonePrefix) {
        private String postalCode(String suffix) {
            return switch (slug) {
                case "tehran" -> "1111111" + suffix;
                case "isfahan" -> "2222222" + suffix;
                case "fars" -> "3333333" + suffix;
                case "khorasan" -> "4444444" + suffix;
                case "azarbayjan" -> "5555555" + suffix;
                default -> "9999999" + suffix;
            };
        }
    }
}
