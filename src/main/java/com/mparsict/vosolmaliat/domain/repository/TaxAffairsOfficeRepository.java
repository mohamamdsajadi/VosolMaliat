package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxAffairsOfficeRepository extends JpaRepository<TaxAffairsOffice, Long> {
    List<TaxAffairsOffice> findByGeneralDirectorateOfTaxAffairsId(Long generalDirectorateOfTaxAffairsId);
}
