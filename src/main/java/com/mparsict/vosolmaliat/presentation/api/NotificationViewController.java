package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.service.InformProcessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/execution-papers") @RequiredArgsConstructor
public class NotificationViewController {
    private final InformProcessService service;
    public record ViewRequest(@NotBlank(message="تاریخ رویت الزامی است") String viewDate, String correctionReason) {}
    @PutMapping("/{id}/notification/view-date")
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable Long id, @RequestBody @Valid ViewRequest request) {
        service.recordOrCorrectView(id, request.viewDate(), request.correctionReason());
    }
}
