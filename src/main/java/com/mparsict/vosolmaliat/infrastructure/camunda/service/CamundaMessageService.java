package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaMessageService {

    private final CamundaClient camundaClient;

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
}
