package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxpayerRepository extends JpaRepository<Taxpayer, Long> {
    Optional<Taxpayer> findByTin(String tin);
    @org.springframework.data.jpa.repository.Query("select t from Taxpayer t where t.legalPerson.id = :id")
    Optional<Taxpayer> findByLegalPersonId(@org.springframework.data.repository.query.Param("id") Long id);


}
