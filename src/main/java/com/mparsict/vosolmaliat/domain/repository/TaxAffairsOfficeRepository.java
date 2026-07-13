package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxAffairsOfficeRepository extends JpaRepository<TaxAffairsOffice, Long> {
    @Query("SELECT o FROM TaxAffairsOffice o WHERE o.generalDirectorateOfTaxAffairs.id = :generalDirectorateOfTaxAffairsId")
    List<TaxAffairsOffice> findByGeneralDirectorateOfTaxAffairsId(@Param("generalDirectorateOfTaxAffairsId") Long generalDirectorateOfTaxAffairsId);
}
