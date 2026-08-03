package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.FakeElectronicNotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FakeElectronicNotificationRequestRepository extends JpaRepository<FakeElectronicNotificationRequest, Long> {

    List<FakeElectronicNotificationRequest> findAllByOrderByIdDesc();

    List<FakeElectronicNotificationRequest> findAllByExecutionPaper_IdOrderByIdDesc(Long executionPaperId);

    Optional<FakeElectronicNotificationRequest> findFirstByExecutionPaper_IdOrderByIdDesc(Long executionPaperId);
}
