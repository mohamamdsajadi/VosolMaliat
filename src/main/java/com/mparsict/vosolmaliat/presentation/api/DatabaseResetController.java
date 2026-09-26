package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.DatabaseResetRequest;
import com.mparsict.vosolmaliat.application.dto.response.DatabaseResetResponse;
import com.mparsict.vosolmaliat.application.service.DatabaseResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/test/database")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "app.test.database-reset.enabled", havingValue = "true")
public class DatabaseResetController {
    private final DatabaseResetService databaseResetService;

    @PostMapping("/reset")
    public DatabaseResetResponse reset(@RequestBody @Valid DatabaseResetRequest request) throws SQLException {
        return databaseResetService.reset();
    }
}
