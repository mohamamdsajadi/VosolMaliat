package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FakeRestrictionRequestRepository extends JpaRepository<FakeRestrictionRequest, Long> {

    List<FakeRestrictionRequest> findAllByOrderByIdDesc();

    List<FakeRestrictionRequest> findAllByExecutionPaperIdOrderByIdDesc(Long executionPaperId);

    Optional<FakeRestrictionRequest> findByOfficialGazetteRequestId(String officialGazetteRequestId);
}
