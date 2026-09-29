package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.PaperTypeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperTypeDefinitionRepository extends JpaRepository<PaperTypeDefinition, Long> {
    boolean existsByCode(String code);
}
