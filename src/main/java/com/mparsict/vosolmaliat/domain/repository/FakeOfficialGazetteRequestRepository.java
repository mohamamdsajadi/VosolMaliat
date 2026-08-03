package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.FakeOfficialGazetteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FakeOfficialGazetteRequestRepository
        extends JpaRepository<FakeOfficialGazetteRequest, Long> {

    List<FakeOfficialGazetteRequest> findAllByOrderByIdDesc();

    List<FakeOfficialGazetteRequest> findAllByExecutionPaperIdOrderByIdDesc(Long executionPaperId);

    Optional<FakeOfficialGazetteRequest> findByRequestId(String requestId);
}
