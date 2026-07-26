package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.RelatedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelatedPersonRepository extends JpaRepository<RelatedPerson, Long> {

    @Query("""
            SELECT relationship
            FROM RelatedPerson relationship
            WHERE relationship.ownerLegalPerson.id = :ownerLegalPersonId
            ORDER BY relationship.startDate ASC, relationship.id ASC
            """)
    List<RelatedPerson> findByOwnerLegalPersonIdOrderByStartDateAscIdAsc(
            @Param("ownerLegalPersonId") Long ownerLegalPersonId);

    @Query("""
            SELECT relationship
            FROM RelatedPerson relationship
            WHERE relationship.ownerLegalPerson.id = :ownerLegalPersonId
              AND relationship.sourceSystem = :sourceSystem
              AND relationship.sourceRecordId = :sourceRecordId
            """)
    Optional<RelatedPerson> findByOwnerLegalPersonIdAndSourceSystemAndSourceRecordId(
            @Param("ownerLegalPersonId") Long ownerLegalPersonId,
            @Param("sourceSystem") String sourceSystem,
            @Param("sourceRecordId") String sourceRecordId);
}
