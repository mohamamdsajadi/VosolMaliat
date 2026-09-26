package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalPersonRepository extends JpaRepository<LegalPerson, Long> {

    Optional<LegalPerson> findByNationalId(String nationalId);

    @org.springframework.data.jpa.repository.Query("select p from LegalPerson p join p.extraInfo e where e.registrationNumber = :number")
    Optional<LegalPerson> findByRegistrationNumber(@org.springframework.data.repository.query.Param("number") String registrationNumber);
}
