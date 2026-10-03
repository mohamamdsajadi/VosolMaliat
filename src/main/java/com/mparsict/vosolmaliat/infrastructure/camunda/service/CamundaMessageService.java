package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaMessageService {

    private final CamundaClient camundaClient;

    public void publishNotificationViewed(Long executionPaperId, Long version, String viewDate) {
        camundaClient.newPublishMessageCommand().messageName("ExecutionPaperViewed")
                .correlationKey(executionPaperId.toString())
                .messageId("execution-view-" + executionPaperId + "-" + version)
                .timeToLive(Duration.ofDays(30))
                .variables(Map.of("taxpayerNotificationViewDate", viewDate)).send().join();
    }

    public void publishExecutionPaperInformed(Long executionPaperId, String informDate) {
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName(ProcessConstants.MESSAGE_EXECUTION_PAPER_INFORMED)
                    .correlationKey(String.valueOf(executionPaperId))
                    .variables(Map.of(ProcessConstants.VAR_INFORM_DATE, informDate))
                    .send()
                    .join();
            log.info("Published Camunda message {} for executionPaperId={}",
                    ProcessConstants.MESSAGE_EXECUTION_PAPER_INFORMED, executionPaperId);
        } catch (Exception ex) {
            throw new BusinessException(
                    "CAMUNDA_MESSAGE_PUBLISH_FAILED",
                    "خطا در ارسال پیام ابلاغ به Camunda: " + ex.getMessage(),
                    ex
            );
        }
    }

    public void publishOfficialGazetteRelatedPersons(
            String requestId,
            List<CompanyRegistryRelatedPersonData> relatedPersons) {
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName(ProcessConstants.MESSAGE_OFFICIAL_GAZETTE_RELATED_PERSONS_RECEIVED)
                    .correlationKey(requestId)
                    .variables(Map.of(
                            ProcessConstants.VAR_OFFICIAL_GAZETTE_RELATED_PERSONS,
                            relatedPersons
                    ))
                    .send()
                    .join();
            log.info("Published official-gazette response for requestId={}", requestId);
        } catch (Exception ex) {
            throw new BusinessException(
                    "CAMUNDA_OFFICIAL_GAZETTE_MESSAGE_FAILED",
                    "خطا در ارسال پاسخ روزنامه رسمی به Camunda: " + ex.getMessage(),
                    ex
            );
        }
    }

    public void publishRestrictionsApplied(String restrictionRequestId) {
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName(ProcessConstants.MESSAGE_RESTRICTIONS_APPLIED)
                    .correlationKey(restrictionRequestId)
                    .messageId("restrictions-applied-" + restrictionRequestId)
                    .timeToLive(Duration.ofHours(1))
                    .variables(Map.of(ProcessConstants.VAR_RESTRICTIONS_APPLIED, true))
                    .send()
                    .join();
            log.info("Published restriction confirmation for restrictionRequestId={}", restrictionRequestId);
        } catch (Exception ex) {
            throw new BusinessException(
                    "CAMUNDA_RESTRICTION_MESSAGE_FAILED",
                    "خطا در ارسال تأیید اعمال محدودیت به Camunda: " + ex.getMessage(),
                    ex
            );
        }
    }

    public void publishTaxpayerDebt(String requestId, SanimDebtStatusResponse debtStatus) {
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName(ProcessConstants.MESSAGE_TAXPAYER_DEBT)
                    .correlationKey(requestId)
                    .messageId("taxpayer-debt-" + requestId)
                    .timeToLive(Duration.ofHours(1))
                    .variables(Map.of(ProcessConstants.VAR_SANIM_DEBT_STATUS, debtStatus))
                    .send()
                    .join();
            log.info("Published SANIM debt status for requestId={}", requestId);
        } catch (Exception ex) {
            throw new BusinessException(
                    "CAMUNDA_TAXPAYER_DEBT_MESSAGE_FAILED",
                    "خطا در ارسال پاسخ بدهی سنیم به Camunda: " + ex.getMessage(),
                    ex
            );
        }
    }
}
