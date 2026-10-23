package com.mparsict.vosolmaliat.domain.repository;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DebtInformationHistoryRepository extends JpaRepository<DebtInformationHistory, Long> {
    java.util.List<DebtInformationHistory> findByFinalTaxDebtPaperIdOrderByCreatedAtAscIdAsc(Long id);
    java.util.Optional<DebtInformationHistory> findFirstByFinalTaxDebtPaperIdAndNextInquiryDateIsNullOrderByIdDesc(Long id);
}
