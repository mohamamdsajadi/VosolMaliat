package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.model.masterdata.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class EnumLookupDataInitializer implements ApplicationRunner {
    private final EntityManager entityManager;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        for (var value : FinalizationReason.values()) persist(FinalizationReasonDefinition.class, value.id(), new FinalizationReasonDefinition(value));
        for (var value : TaxSourceType.values()) persist(TaxSourceDefinition.class, value.id(), new TaxSourceDefinition(value));
        for (var value : TaxPeriod.values()) persist(TaxPeriodDefinition.class, value.id(), new TaxPeriodDefinition(value));
        for (var value : TaxpayerType.values()) persist(TaxpayerTypeDefinition.class, value.id(), new TaxpayerTypeDefinition(value));
        for (var value : RestrictionState.values()) persist(RestrictionStateDefinition.class, value.id(), new RestrictionStateDefinition(value));
        for (var value : RuleScope.values()) persist(RuleScopeDefinition.class, value.id(), new RuleScopeDefinition(value));
        for (var value : ExecutionPaperState.values()) persist(ExecutionPaperStateDefinition.class, value.id(), new ExecutionPaperStateDefinition(value));
    }

    private <T> void persist(Class<T> type, int id, T value) {
        if (entityManager.find(type, id) == null) entityManager.persist(value);
    }
}
