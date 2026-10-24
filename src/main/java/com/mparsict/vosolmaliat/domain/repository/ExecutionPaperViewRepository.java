package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExecutionPaperViewRepository extends JpaRepository<ExecutionPaperView, ExecutionPaperView.Key> {
    List<ExecutionPaperView> findByUserKeyAndExecutionPaper_IdIn(String userKey, List<Long> paperIds);
    Optional<ExecutionPaperView> findByUserKeyAndExecutionPaper_Id(String userKey, Long paperId);

    // One row per user/paper, including simultaneous opens in multiple tabs.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        INSERT INTO execution_paper_view (user_key, execution_paper_id, last_viewed_at_millis)
        VALUES (:userKey, :paperId, :viewedAt)
        ON CONFLICT (user_key, execution_paper_id) DO UPDATE
        SET last_viewed_at_millis = MAX(execution_paper_view.last_viewed_at_millis, excluded.last_viewed_at_millis)
        """, nativeQuery = true)
    void recordView(@Param("userKey") String userKey, @Param("paperId") Long paperId, @Param("viewedAt") long viewedAt);
}
