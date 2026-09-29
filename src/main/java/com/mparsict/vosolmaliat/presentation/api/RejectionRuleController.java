package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.RejectionRuleRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/rejection-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_ASSIGN')")
public class RejectionRuleController {

    private final ExecutionPaperRejectionRuleRepository ruleRepository;

    @GetMapping
    public List<ExecutionPaperRejectionRule> getAll() {
        log.debug("GET /api/v1/admin/rejection-rules");
        return ruleRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<ExecutionPaperRejectionRule> create(
            @RequestBody @Valid RejectionRuleRequest request) {
        log.debug("POST /api/v1/admin/rejection-rules");
        ExecutionPaperRejectionRule rule = ExecutionPaperRejectionRule.builder()
                .ruleJson(request.ruleJson())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .documentNo(request.documentNo())
                .filePath(request.filePath())
                .description(request.description())
                .build();
        ExecutionPaperRejectionRule saved = ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExecutionPaperRejectionRule> update(
            @PathVariable Long id,
            @RequestBody @Valid RejectionRuleRequest request) {
        log.debug("PUT /api/v1/admin/rejection-rules/{}", id);
        ExecutionPaperRejectionRule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("RULE_NOT_FOUND", "قانون رد با شناسه " + id + " یافت نشد"));
        existing.update(request.ruleJson(), request.startDate(), request.endDate(),
                request.documentNo(), request.filePath(), request.description());
        return ResponseEntity.ok(ruleRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("DELETE /api/v1/admin/rejection-rules/{}", id);
        if (!ruleRepository.existsById(id)) {
            throw new BusinessException("RULE_NOT_FOUND", "قانون رد با شناسه " + id + " یافت نشد");
        }
        ExecutionPaperRejectionRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("RULE_NOT_FOUND", "قانون رد با شناسه " + id + " یافت نشد"));
        rule.deactivate();
        ruleRepository.save(rule);
        return ResponseEntity.noContent().build();
    }
}
