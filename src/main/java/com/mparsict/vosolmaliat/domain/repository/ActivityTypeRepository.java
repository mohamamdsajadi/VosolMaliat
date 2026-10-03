package com.mparsict.vosolmaliat.domain.repository;
import com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    java.util.Optional<ActivityType> findByIntaCode(String code);
    java.util.List<ActivityType> findByTitle(String title);
}
