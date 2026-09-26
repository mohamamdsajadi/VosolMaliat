package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {

    @Query("SELECT c FROM ContactInfo c WHERE c.naturalPerson.id = :naturalPersonId")
    Optional<ContactInfo> findFirstByNaturalPersonId(@Param("naturalPersonId") Long naturalPersonId);

    @Query("SELECT c FROM ContactInfo c WHERE c.legalPerson.id = :legalPersonId")
    Optional<ContactInfo> findFirstByLegalPersonId(@Param("legalPersonId") Long legalPersonId);
}
