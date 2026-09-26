package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxUnitRepository extends JpaRepository<TaxUnit, Long> {
    @Query("SELECT u FROM TaxUnit u WHERE u.taxAffairsOffice.id = :taxAffairsOfficeId")
    List<TaxUnit> findByTaxAffairsOfficeId(@Param("taxAffairsOfficeId") Long taxAffairsOfficeId);

    @Query("SELECT office.id FROM TaxUnit unit JOIN unit.taxAffairsOffice office WHERE unit.id = :taxUnitId")
    Optional<Long> findTaxAffairsOfficeIdByTaxUnitId(@Param("taxUnitId") Long taxUnitId);

    @Query("""
            SELECT general.id
            FROM TaxUnit unit
            JOIN unit.taxAffairsOffice office
            JOIN office.generalDirectorateOfTaxAffairs general
            WHERE unit.id = :taxUnitId
            """)
    Optional<Long> findGeneralDirectorateIdByTaxUnitId(@Param("taxUnitId") Long taxUnitId);
}
