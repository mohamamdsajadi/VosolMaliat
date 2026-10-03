package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.RejectionRuleRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperInformRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperInformRejectionRuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@org.springframework.transaction.annotation.Transactional
@RestController
@RequestMapping("/api/v1/admin/inform-rejection-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PERMISSION_ASSIGN')")
public class InformRejectionRuleController {

    private final ExecutionPaperInformRejectionRuleRepository ruleRepository;

    @GetMapping
    public List<ExecutionPaperInformRejectionRule> getAll() {
        return ruleRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<ExecutionPaperInformRejectionRule> create(@RequestBody @Valid RejectionRuleRequest request) {
        ExecutionPaperInformRejectionRule rule = ExecutionPaperInformRejectionRule.builder()
                .ruleJson(request.ruleJson())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .documentNo(request.documentNo())
                .filePath(request.filePath())
                .description(request.description())
                .errorCode(request.errorCode()).errorMessage(request.errorMessage())
                .build();
        if (request.parentRuleId() != null) {
            var parent = ruleRepository.findById(request.parentRuleId())
                    .orElseThrow(() -> new BusinessException("RULE_NOT_FOUND", "قاعده والد یافت نشد"));
            if (!Boolean.TRUE.equals(parent.getActive()))
                throw new BusinessException("RULE_INACTIVE", "قاعده والد غیرفعال است");
            parent.deactivate();
            parent.recordVersion();
            ruleRepository.save(parent);
            rule.setParentRule(parent);
        }
        rule.recordVersion();
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleRepository.save(rule));
    }

    @PutMapping("/{id}")
    public ExecutionPaperInformRejectionRule update(@PathVariable Long id, @RequestBody @Valid RejectionRuleRequest request) {
        ExecutionPaperInformRejectionRule existing = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("INFORM_RULE_NOT_FOUND", "قانون ابلاغ با شناسه " + id + " یافت نشد"));
        existing.update(request.ruleJson(), request.startDate(), request.endDate(),
                request.documentNo(), request.filePath(), request.description());
        existing.setErrorCode(request.errorCode());
        existing.setErrorMessage(request.errorMessage());
        existing.recordVersion();
        return ruleRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new BusinessException("INFORM_RULE_NOT_FOUND", "قانون ابلاغ با شناسه " + id + " یافت نشد");
        }
        ExecutionPaperInformRejectionRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("INFORM_RULE_NOT_FOUND", "قانون ابلاغ با شناسه " + id + " یافت نشد"));
        rule.deactivate();
        rule.recordVersion();
        ruleRepository.save(rule);
        return ResponseEntity.noContent().build();
    }
}
