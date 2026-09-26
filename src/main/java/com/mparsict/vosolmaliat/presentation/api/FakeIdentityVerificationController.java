package com.mparsict.vosolmaliat.presentation.api;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.domain.repository.DebtPaperPushRecordRepository;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin/identity-verifications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
public class FakeIdentityVerificationController {
    private final DebtPaperPushRecordRepository records;
    private final FakeIdentityVerificationService identities;
    private final DebtPaperPushService push;
    @GetMapping
    public List<Map<String,Object>> list() {
        return records.findAll(PageRequest.of(0,100,Sort.by("id").descending())).stream().map(r -> {
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("id",r.getId()); item.put("referenceId",r.getReferenceId());
            item.put("identifier",r.getNationalId()); item.put("followCode",r.getFollowCode());
            item.put("status",r.getProcessingStatus()); item.put("reason",r.getFailureReason());
            item.put("executionPaperId",r.getExecutionPaperId());
            return item;
        }).toList();
    }
    @PostMapping("/{id}/decision")
    public Map<String,Object> decide(@PathVariable Long id, @RequestBody FakeIdentityVerificationService.Decision decision) {
        var receipt=records.findById(id).orElseThrow();
        if (receipt.getProcessingStatus()!=DebtPaperProcessingStatus.WAITING_IDENTITY)
            throw new com.mparsict.vosolmaliat.common.exception.BusinessException("NOT_WAITING_IDENTITY","درخواست در انتظار هویت نیست");
        identities.decide(normalize(receipt.getNationalId()),decision);
        try {
            Long paperId=push.resume(id);
            Map<String,Object> result=new LinkedHashMap<>(); result.put("executionPaperId",paperId);
            result.put("status",records.findById(id).orElseThrow().getProcessingStatus()); return result;
        } catch (com.mparsict.vosolmaliat.common.exception.BusinessException ex) {
            if (!"IDENTITY_REJECTED".equals(ex.getErrorCode())) throw ex;
            return Map.of("status","REJECTED_IDENTITY");
        }
    }
    private String normalize(String value) {
        StringBuilder result=new StringBuilder();
        for(char c:value.trim().toCharArray()) result.append(Character.isDigit(c) ? (char)('0'+Character.digit(c,10)) : c);
        return result.toString();
    }
}
