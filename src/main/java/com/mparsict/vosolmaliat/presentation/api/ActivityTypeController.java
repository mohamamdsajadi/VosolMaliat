package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.domain.model.masterdata.*;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController @RequestMapping("/api/v1/masterdata/activity-types")
@RequiredArgsConstructor @Transactional
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_ASSIGN')")
public class ActivityTypeController {
    private final ActivityTypeRepository repository;
    private final ActivityTypeHistoryRepository history;
    public record Request(@NotBlank(message="اینتاکد الزامی است") String intaCode,
                          @NotBlank(message="عنوان فعالیت الزامی است") String title) {}
    @GetMapping public List<ActivityType> list() { return repository.findAll(); }
    @PostMapping public ActivityType create(@RequestBody @Valid Request request) {
        return repository.save(ActivityType.builder().intaCode(request.intaCode()).title(request.title()).build());
    }
    @PutMapping("/{id}") public ActivityType update(@PathVariable Long id, @RequestBody @Valid Request request) {
        var row = find(id); history.save(ActivityTypeHistory.from(row));
        row.setIntaCode(request.intaCode()); row.setTitle(request.title());
        return repository.save(row);
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) {
        var row=find(id); history.save(ActivityTypeHistory.from(row)); repository.delete(row);
    }
    private ActivityType find(Long id) { return repository.findById(id).orElseThrow(() ->
        new BusinessException("ACTIVITY_TYPE_NOT_FOUND", "نوع فعالیت یافت نشد")); }
}
