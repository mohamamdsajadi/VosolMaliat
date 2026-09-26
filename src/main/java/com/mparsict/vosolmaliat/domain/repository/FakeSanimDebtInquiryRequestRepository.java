package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimDebtInquiryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FakeSanimDebtInquiryRequestRepository
        extends JpaRepository<FakeSanimDebtInquiryRequest, Long> {
    Optional<FakeSanimDebtInquiryRequest> findByRequestId(String requestId);
    List<FakeSanimDebtInquiryRequest> findAllByOrderByIdDesc();
}
