package com.mparsict.vosolmaliat.domain.repository;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DebtInformationHistoryRepository extends JpaRepository<DebtInformationHistory, Long> {
    java.util.Optional<DebtInformationHistory> findFirstByDebtInformationIdAndNextInquiryDateIsNullOrderByIdDesc(Long id);
}
