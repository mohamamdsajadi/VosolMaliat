package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.FakeCompanyRegistryService;
import com.mparsict.vosolmaliat.application.service.RelatedPersonService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPerson;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPersonPost;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalTaxDebtPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonPostRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedPersonServiceUnitTest {

    @Test
    void refreshUpsertsPeopleAndRelationshipsAndFiltersThemByJalaliDebtYear() {
        RepositoryState state = new RepositoryState("1405");
        StubAuditLogService auditLogService = new StubAuditLogService();
        RelatedPersonService service = state.service(auditLogService);

        var firstRefresh = service.refreshForExecutionPaper(100L);
        var secondRefresh = service.refreshForExecutionPaper(100L);

        assertThat(firstRefresh.debtYear()).isEqualTo("1405");
        assertThat(firstRefresh.relatedPersons()).hasSizeBetween(1, 2);
        assertThat(secondRefresh.relatedPersons().size() - firstRefresh.relatedPersons().size())
                .isBetween(1, 2);
        assertThat(state.relationships).hasSize(secondRefresh.relatedPersons().size());
        assertThat(state.relationships)
                .filteredOn(item -> item.getEndDate() != null)
                .hasSizeBetween(1, Math.min(2, firstRefresh.relatedPersons().size()));
        assertThat(secondRefresh.relatedPersons())
                .filteredOn(item -> item.personType().equals(RelatedPersonType.LEGAL.name()))
                .allSatisfy(item -> {
                    assertThat(item.legalRepresentativeNaturalPersonId()).isNotNull();
                    assertThat(item.legalRepresentativeNationalCode()).isNotBlank();
                });
        assertThat(secondRefresh.relatedPersons())
                .filteredOn(item -> item.personType().equals(RelatedPersonType.NATURAL.name()))
                .allSatisfy(item -> assertThat(item.legalRepresentativeNaturalPersonId()).isNull());
        assertThat(secondRefresh.relatedPersons())
                .extracting(item -> item.postId())
                .doesNotContainNull();
        assertThat(secondRefresh.relatedPersons())
                .allSatisfy(item -> assertThat(
                        RelatedPersonPostCode.fromCompanyRegistrationCode(item.postCode())
                                .supports(RelatedPersonType.valueOf(item.personType()))
                ).isTrue());
        assertThat(auditLogService.action)
                .isEqualTo("RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY");
        assertThat(auditLogService.newValue).contains("\"debtYear\":\"1405\"");
    }

    @Test
    void retrievalIncludesOnlyRelationshipsOverlappingDebtYear() {
        RepositoryState state = new RepositoryState("1399");
        RelatedPersonService service = state.service(new StubAuditLogService());
        service.refreshForExecutionPaper(100L);

        var response = service.getForExecutionPaper(100L);

        assertThat(response.relatedPersons())
                .hasSizeBetween(1, 2)
                .allSatisfy(item -> assertThat(
                        RelatedPersonPostCode.fromCompanyRegistrationCode(item.postCode())
                                .supports(RelatedPersonType.valueOf(item.personType()))
                ).isTrue());
    }

    @Test
    void postsAreRestrictedByRelatedPersonType() {
        assertThat(RelatedPersonPostCode.CEO.supports(RelatedPersonType.NATURAL)).isTrue();
        assertThat(RelatedPersonPostCode.CEO.supports(RelatedPersonType.LEGAL)).isFalse();
        assertThat(RelatedPersonPostCode.PARENT_COMPANY.supports(RelatedPersonType.NATURAL)).isFalse();
        assertThat(RelatedPersonPostCode.PARENT_COMPANY.supports(RelatedPersonType.LEGAL)).isTrue();
        assertThat(RelatedPersonPostCode.SHAREHOLDER.supports(RelatedPersonType.NATURAL)).isTrue();
        assertThat(RelatedPersonPostCode.SHAREHOLDER.supports(RelatedPersonType.LEGAL)).isTrue();
    }

    private static class RepositoryState {
        private final LegalPerson owner = LegalPerson.builder()
                .id(10L)
                .nationalId("14001234567")
                .registrationNumber("12345")
                .registrationDate("1390-01-01")
                .name("شرکت مودی")
                .build();
        private final FinalTaxDebtPaper finalTaxDebtPaper;
        private final ExecutionPaper executionPaper;
        private final Taxpayer taxpayer;
        private final Map<String, NaturalPerson> naturalPeopleByNationalCode = new HashMap<>();
        private final Map<String, LegalPerson> legalPeopleByNationalId = new HashMap<>();
        private final Map<RelatedPersonPostCode, RelatedPersonPost> postsByCode = new HashMap<>();
        private final List<RelatedPerson> relationships = new ArrayList<>();
        private long nextNaturalId = 20;
        private long nextLegalId = 30;
        private long nextRelationshipId = 40;

        private RepositoryState(String debtYear) {
            long postId = 1;
            for (RelatedPersonPostCode code : RelatedPersonPostCode.values()) {
                if (code.hasCompanyRegistrationCode()) {
                    postsByCode.put(code, RelatedPersonPost.builder().id(postId++).code(code)
                            .codeInCompanyRegistrationSystem(code.companyRegistrationCode())
                            .title(code.label()).build());
                }
            }
            legalPeopleByNationalId.put(owner.getNationalId(), owner);
            taxpayer = Taxpayer.builder().id(3L).legalPerson(owner).build();
            finalTaxDebtPaper = FinalTaxDebtPaper.builder()
                    .id(2L)
                    .taxYear(Integer.valueOf(debtYear))
                    .taxpayer(taxpayer)
                    .build();
            executionPaper = ExecutionPaper.builder()
                    .id(100L)
                    .finalTaxDebtPaper(finalTaxDebtPaper)
                    .build();
        }

        private RelatedPersonService service(StubAuditLogService auditLogService) {
            return new RelatedPersonService(
                    executionPaperRepository(),
                    taxpayerRepository(),
                    finalTaxDebtPaperRepository(),
                    naturalPersonRepository(),
                    legalPersonRepository(),
                    relatedPersonPostRepository(),
                    relatedPersonRepository(),
                    new FakeCompanyRegistryService(),
                    auditLogService,
                    new ObjectMapper()
            );
        }

        private RelatedPersonPostRepository relatedPersonPostRepository() {
            return proxy(RelatedPersonPostRepository.class, (method, args) ->
                    "findByCode".equals(method) ? Optional.ofNullable(postsByCode.get(args[0])) : null);
        }

        private ExecutionPaperRepository executionPaperRepository() {
            return proxy(ExecutionPaperRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(executionPaper) : null);
        }

        private TaxpayerRepository taxpayerRepository() {
            return proxy(TaxpayerRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(taxpayer) : null);
        }

        private FinalTaxDebtPaperRepository finalTaxDebtPaperRepository() {
            return proxy(FinalTaxDebtPaperRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(finalTaxDebtPaper) : null);
        }

        private NaturalPersonRepository naturalPersonRepository() {
            return proxy(NaturalPersonRepository.class, (method, args) -> switch (method) {
                case "findByNationalCode" -> Optional.ofNullable(naturalPeopleByNationalCode.get(args[0]));
                case "findByFida" -> Optional.empty();
                case "save" -> {
                    NaturalPerson person = (NaturalPerson) args[0];
                    if (person.getId() == null) {
                        person.setId(nextNaturalId++);
                    }
                    naturalPeopleByNationalCode.put(person.getNationalCode(), person);
                    yield person;
                }
                default -> null;
            });
        }

        private LegalPersonRepository legalPersonRepository() {
            return proxy(LegalPersonRepository.class, (method, args) -> switch (method) {
                case "findById" -> legalPeopleByNationalId.values().stream()
                        .filter(person -> person.getId().equals(args[0]))
                        .findFirst();
                case "findByNationalId" -> Optional.ofNullable(legalPeopleByNationalId.get(args[0]));
                case "findByRegistrationNumber" -> Optional.empty();
                case "save" -> {
                    LegalPerson person = (LegalPerson) args[0];
                    if (person.getId() == null) {
                        person.setId(nextLegalId++);
                    }
                    legalPeopleByNationalId.put(person.getNationalId(), person);
                    yield person;
                }
                default -> null;
            });
        }

        private RelatedPersonRepository relatedPersonRepository() {
            return proxy(RelatedPersonRepository.class, (method, args) -> switch (method) {
                case "findByOwnerLegalPersonIdOrderByStartDateAscIdAsc" -> relationships.stream()
                        .sorted(java.util.Comparator
                                .comparing(RelatedPerson::getStartDate)
                                .thenComparing(RelatedPerson::getId))
                        .toList();
                case "findByOwnerLegalPersonIdAndPostIdAndNaturalPersonIdAndStartDate" -> relationships.stream()
                        .filter(item -> item.getOwnerLegalPersonId().equals(args[0])
                                && item.getPost().getId().equals(args[1])
                                && item.getNaturalPerson() != null && item.getNaturalPerson().getId().equals(args[2])
                                && java.util.Objects.equals(item.getStartDate(), args[3]))
                        .findFirst();
                case "findByOwnerLegalPersonIdAndPostIdAndLegalPersonIdAndStartDate" -> relationships.stream()
                        .filter(item -> item.getOwnerLegalPersonId().equals(args[0])
                                && item.getPost().getId().equals(args[1])
                                && item.getLegalPerson() != null && item.getLegalPerson().getId().equals(args[2])
                                && java.util.Objects.equals(item.getStartDate(), args[3]))
                        .findFirst();
                case "save" -> {
                    RelatedPerson relationship = (RelatedPerson) args[0];
                    if (relationship.getId() == null) {
                        relationship.setId(nextRelationshipId++);
                        relationships.add(relationship);
                    }
                    yield relationship;
                }
                default -> null;
            });
        }
    }

    private static class StubAuditLogService extends AuditLogService {
        private String action;
        private String newValue;

        private StubAuditLogService() {
            super(null);
        }

        @Override
        public void record(
                CurrentUserContext actor,
                String action,
                String targetType,
                String targetId,
                String oldValue,
                String newValue) {
            this.action = action;
            this.newValue = newValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, RepositoryInvocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object value = invocation.invoke(method.getName(), args == null ? new Object[0] : args);
                    return value != null ? value : defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object invoke(String method, Object[] args);
    }
}
