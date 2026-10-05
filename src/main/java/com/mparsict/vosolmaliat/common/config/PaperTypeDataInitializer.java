package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.masterdata.PaperTypeDefinition;
import com.mparsict.vosolmaliat.domain.repository.PaperTypeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
// The audit columns on this table reference the reserved system actor.  Seed
// that actor first so SQLite foreign-key enforcement cannot make startup
// ordering nondeterministic.
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PaperTypeDataInitializer implements ApplicationRunner {
    private final PaperTypeDefinitionRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PaperType type : PaperType.values()) {
            if (!repository.existsByCode(type.code())) {
                repository.save(PaperTypeDefinition.builder()
                        .code(type.code()).paperTitle(type.getPersianTitle()).build());
            }
        }
    }
}
