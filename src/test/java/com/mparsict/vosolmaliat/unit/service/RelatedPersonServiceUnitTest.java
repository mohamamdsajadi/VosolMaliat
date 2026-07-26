package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.FakeCompanyRegistryService;
import com.mparsict.vosolmaliat.application.service.RelatedPersonService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonRole;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPerson;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalizedDebtPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonRepository;
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
        assertThat(firstRefresh.relatedPersons()).hasSize(4);
        assertThat(secondRefresh.relatedPersons()).hasSize(4);
        assertThat(state.relationships).hasSize(4);
        assertThat(state.naturalPeopleByNationalCode).hasSize(2);
        assertThat(state.legalPeopleByNationalId).hasSize(2);
        assertThat(firstRefresh.relatedPersons())
                .filteredOn(item -> item.personType().equals(RelatedPersonType.NATURAL.name()))
                .extracting(item -> item.role())
                .contains(
                        RelatedPersonRole.CEO.name(),
                        RelatedPersonRole.BOARD_MEMBER.name(),
                        RelatedPersonRole.BOARD_CHAIR.name()
                );
        assertThat(firstRefresh.relatedPersons())
                .filteredOn(item -> item.personType().equals(RelatedPersonType.LEGAL.name()))
                .extracting(item -> item.role())
                .containsExactly(RelatedPersonRole.SHAREHOLDER.name());
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
                .extracting(item -> item.role())
                .containsExactlyInAnyOrder(
                        RelatedPersonRole.BOARD_MEMBER.name(),
                        RelatedPersonRole.SHAREHOLDER.name()
                );
    }

    @Test
    void rolesAreRestrictedByRelatedPersonType() {
        assertThat(RelatedPersonRole.CEO.supports(RelatedPersonType.NATURAL)).isTrue();
        assertThat(RelatedPersonRole.CEO.supports(RelatedPersonType.LEGAL)).isFalse();
        assertThat(RelatedPersonRole.PARENT_COMPANY.supports(RelatedPersonType.NATURAL)).isFalse();
        assertThat(RelatedPersonRole.PARENT_COMPANY.supports(RelatedPersonType.LEGAL)).isTrue();
        assertThat(RelatedPersonRole.SHAREHOLDER.supports(RelatedPersonType.NATURAL)).isTrue();
        assertThat(RelatedPersonRole.SHAREHOLDER.supports(RelatedPersonType.LEGAL)).isTrue();
    }

    private static class RepositoryState {
        private final LegalPerson owner = LegalPerson.builder()
                .id(10L)
                .nationalId("14001234567")
                .registrationNumber("12345")
                .registrationDate("1390-01-01")
                .name("شرکت مودی")
                .build();
        private final FinalizedDebtPaper finalizedDebtPaper;
        private final ExecutionPaper executionPaper;
        private final Taxpayer taxpayer;
        private final Map<String, NaturalPerson> naturalPeopleByNationalCode = new HashMap<>();
        private final Map<String, LegalPerson> legalPeopleByNationalId = new HashMap<>();
        private final List<RelatedPerson> relationships = new ArrayList<>();
        private long nextNaturalId = 20;
        private long nextLegalId = 30;
        private long nextRelationshipId = 40;

        private RepositoryState(String debtYear) {
            legalPeopleByNationalId.put(owner.getNationalId(), owner);
            finalizedDebtPaper = FinalizedDebtPaper.builder()
                    .id(2L)
                    .performanceYear(debtYear)
                    .build();
            taxpayer = Taxpayer.builder().id(3L).legalPerson(owner).build();
            executionPaper = ExecutionPaper.builder()
                    .id(100L)
                    .taxpayer(taxpayer)
                    .finalizedDebtPaper(finalizedDebtPaper)
                    .build();
        }

        private RelatedPersonService service(StubAuditLogService auditLogService) {
            return new RelatedPersonService(
                    executionPaperRepository(),
                    taxpayerRepository(),
                    finalizedDebtPaperRepository(),
                    naturalPersonRepository(),
                    legalPersonRepository(),
                    relatedPersonRepository(),
                    new FakeCompanyRegistryService(),
                    auditLogService,
                    new ObjectMapper()
            );
        }

        private ExecutionPaperRepository executionPaperRepository() {
            return proxy(ExecutionPaperRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(executionPaper) : null);
        }

        private TaxpayerRepository taxpayerRepository() {
            return proxy(TaxpayerRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(taxpayer) : null);
        }

        private FinalizedDebtPaperRepository finalizedDebtPaperRepository() {
            return proxy(FinalizedDebtPaperRepository.class, (method, args) ->
                    "findById".equals(method) ? Optional.of(finalizedDebtPaper) : null);
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
                case "findByOwnerLegalPersonIdAndSourceSystemAndSourceRecordId" -> relationships.stream()
                        .filter(item -> item.getOwnerLegalPersonId().equals(args[0])
                                && item.getSourceSystem().equals(args[1])
                                && item.getSourceRecordId().equals(args[2]))
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
