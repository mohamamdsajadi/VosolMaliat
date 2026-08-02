package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.integration.SanimAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanimAuditEntryRepository extends JpaRepository<SanimAuditEntry, Long> {
    List<SanimAuditEntry> findAllByTaxpayerTypeAndTaxpayerIdentifierOrderByOccurredAtDescIdDesc(
            TaxpayerType taxpayerType, String taxpayerIdentifier);
}
