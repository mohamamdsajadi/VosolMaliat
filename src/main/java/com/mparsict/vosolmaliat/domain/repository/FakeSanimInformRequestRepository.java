package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FakeSanimInformRequestRepository extends JpaRepository<FakeSanimInformRequest, Long> {

    List<FakeSanimInformRequest> findAllByOrderByIdDesc();

    List<FakeSanimInformRequest> findAllByExecutionPaper_IdOrderByIdDesc(Long executionPaperId);
}
