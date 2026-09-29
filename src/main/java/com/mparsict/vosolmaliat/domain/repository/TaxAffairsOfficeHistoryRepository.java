package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOfficeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxAffairsOfficeHistoryRepository extends JpaRepository<TaxAffairsOfficeHistory, Long> {
    List<TaxAffairsOfficeHistory> findByTaxAffairsOfficeIdOrderByIdAsc(Long id);
}
