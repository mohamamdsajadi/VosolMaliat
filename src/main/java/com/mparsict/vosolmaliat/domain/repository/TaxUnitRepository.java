package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxUnitRepository extends JpaRepository<TaxUnit, Long> {
    @Query("SELECT u FROM TaxUnit u WHERE u.taxAffairsOffice.id = :taxAffairsOfficeId")
    List<TaxUnit> findByTaxAffairsOfficeId(@Param("taxAffairsOfficeId") Long taxAffairsOfficeId);
}
