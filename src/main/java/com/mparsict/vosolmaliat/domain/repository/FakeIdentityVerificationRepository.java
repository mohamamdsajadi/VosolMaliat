package com.mparsict.vosolmaliat.domain.repository;
import com.mparsict.vosolmaliat.domain.model.integration.FakeIdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FakeIdentityVerificationRepository extends JpaRepository<FakeIdentityVerification,String> {}
