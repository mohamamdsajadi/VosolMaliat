package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneralDirectorateOfTaxAffairsHistoryRepository
        extends JpaRepository<GeneralDirectorateOfTaxAffairsHistory, Long> {
    List<GeneralDirectorateOfTaxAffairsHistory> findByGeneralDirectorateIdOrderByIdAsc(Long id);
}
