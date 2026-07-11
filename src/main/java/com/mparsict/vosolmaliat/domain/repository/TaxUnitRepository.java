package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxUnitRepository extends JpaRepository<TaxUnit, Long> {
    List<TaxUnit> findByTaxAffairsOfficeId(Long taxAffairsOfficeId);
}
