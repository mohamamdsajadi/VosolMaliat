package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendRejectionResponseWorker {

    private final IssuanceRequestService issuanceRequestService;
    private final FakeSanimInformService fakeSanimInformService;

    @JobWorker(type = ProcessConstants.JOB_SEND_REJECTION)
    public void handle(final JobClient client, final ActivatedJob job) {
        log.debug("Processing job: inform_sanim_rejection, key={}", job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long requestId = ((Number) variables.get(ProcessConstants.VAR_ISSUANCE_REQUEST_ID)).longValue();
            String rejectionCode = (String) variables.get(ProcessConstants.VAR_REJECTION_CODE);
            String rejectionDesc = (String) variables.get(ProcessConstants.VAR_REJECTION_DESC);
            String restrictionRemovalDate = (String) variables.get(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE);

            String rejectionReason = rejectionCode + ": " + rejectionDesc;
            if (restrictionRemovalDate != null && !restrictionRemovalDate.isBlank()) {
                rejectionReason += " (restriction removal date: " + restrictionRemovalDate + ")";
            }
            issuanceRequestService.markAsRejected(requestId, rejectionReason, null);
            fakeSanimInformService.sendIssuanceRejection(
                    requestId,
                    rejectionCode,
                    rejectionDesc,
                    restrictionRemovalDate
            );

            Map<String, Object> outputVars = new HashMap<>();
            outputVars.put(ProcessConstants.VAR_SUCCESS, false);

            log.info("Rejection recorded and sent to fake SANIM for requestId={}, code={}", requestId, rejectionCode);
            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in SendRejectionResponseWorker", e);
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
        }
    }
}
