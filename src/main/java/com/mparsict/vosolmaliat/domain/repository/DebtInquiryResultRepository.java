package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.inquiry.DebtInquiryResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DebtInquiryResultRepository extends JpaRepository<DebtInquiryResult, Long> {
    Optional<DebtInquiryResult> findFirstByHashAndFinalTaxDebtPaperNotificationId(String hash, String notificationId);
}
