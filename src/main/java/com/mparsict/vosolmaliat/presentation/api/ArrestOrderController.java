package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.ArrestOrderRequests.*;
import com.mparsict.vosolmaliat.application.dto.response.ArrestOrderResponses.*;
import com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderService;
import com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderCommandExecutor;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController @RequestMapping("/api/v1/arrest-orders") @RequiredArgsConstructor
public class ArrestOrderController {
    private final ArrestOrderService service;
    private final ArrestOrderCommandExecutor commands;

    @GetMapping("/candidates")
    public Page<Candidate> candidates(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "") String search) {
        return service.candidates(page, size, search);
    }
    @GetMapping
    public Page<Row> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                          @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String stage,
                          @RequestParam(defaultValue = "false") boolean mine, @RequestParam(required = false) Long executionPaperId,
                          @RequestParam(defaultValue = "false") boolean history) {
        return service.list(page, size, search, stage, mine, executionPaperId, history);
    }
    @GetMapping("/{id}") public Detail detail(@PathVariable Long id) { return service.detail(id); }
    @GetMapping("/targets")
    public List<User> targets(@RequestParam(required = false) Long executionPaperId,
                              @RequestParam(required = false) Long orderId, @RequestParam BusinessRole role) {
        return service.targets(executionPaperId, orderId, role);
    }
    @PostMapping
    public Batch start(@Valid @RequestBody Start request) {
        if (new LinkedHashSet<>(request.executionPaperIds()).size() != request.executionPaperIds().size())
            throw new ResponseStatusException(BAD_REQUEST, "یک برگ اجرا چند بار انتخاب شده است.");
        var results = new ArrayList<StartResult>();
        for (Long paperId : request.executionPaperIds()) {
            try { results.add(new StartResult(paperId, true, commands.execute(() -> service.start(paperId, request)), "فرایند آغاز شد.")); }
            catch (ResponseStatusException ex) { results.add(new StartResult(paperId, false, null, ex.getReason())); }
            catch (AccessDeniedException ex) { results.add(new StartResult(paperId, false, null, "دسترسی به نقش یا اداره کل این برگ اجرا مجاز نیست.")); }
            catch (DataIntegrityViolationException ex) { results.add(new StartResult(paperId, false, null, "برای این برگ اجرا فرایند دیگری ثبت شده است؛ فهرست را بروزرسانی کنید.")); }
        }
        long succeeded = results.stream().filter(StartResult::success).count();
        return new Batch(results, succeeded, results.size() - succeeded);
    }
    @PostMapping("/{id}/prepare") public Row prepare(@PathVariable Long id, @Valid @RequestBody Transition request) { return commands.execute(() -> service.prepare(id, request)); }
    @PostMapping("/{id}/approve") public Row approve(@PathVariable Long id, @Valid @RequestBody Transition request) { return commands.execute(() -> service.approve(id, request)); }
    @PostMapping("/{id}/refresh-draft") public Row refreshDraft(@PathVariable Long id, @Valid @RequestBody Transition request) { return commands.execute(() -> service.refreshDraft(id, request)); }
    @PostMapping("/{id}/assign-officer") public Row assign(@PathVariable Long id, @Valid @RequestBody Transition request) { return commands.execute(() -> service.assignOfficer(id, request)); }
}
