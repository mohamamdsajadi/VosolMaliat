package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;

/** Persisted delivery flag survives broker outages; message IDs make retries idempotent. */
@Service @RequiredArgsConstructor @Slf4j @EnableScheduling
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="camunda.client.enabled", havingValue="true", matchIfMissing=true)
public class NotificationMessageDispatcher {
    private final ExecutionWorkflowStateRepository workflows;
    private final ExecutionPaperNotificationRepository notifications;
    private final CamundaMessageService messages;
    @Scheduled(fixedDelayString="${app.notification.dispatch-delay-ms:5000}")
    @Transactional
    public void dispatch() {
        for (var workflow : workflows.findTop100ByViewDateMessagePendingTrueOrderByIdAsc()) {
            var notification = notifications.findByExecutionPaperId(workflow.getId()).orElseThrow();
            try {
                messages.publishNotificationViewed(workflow.getId(), notification.getVersion(),
                        notification.getTaxpayerNotificationViewDate().toString());
                workflow.setViewDateMessagePending(false);
            } catch (RuntimeException exception) {
                log.warn("Notification view delivery pending for execution paper {}", workflow.getId(), exception);
            }
        }
    }
}
