package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;

final class WorkerFailureHandler {

    private WorkerFailureHandler() {
    }

    static void failSafely(JobClient client, ActivatedJob job, Exception cause, Logger log) {
        try {
            client.newFailCommand(job.getKey())
                    .retries(Math.max(job.getRetries() - 1, 0))
                    .errorMessage(cause.getMessage())
                    .send()
                    .join();
        } catch (Exception failException) {
            log.warn(
                    "Could not fail job type={} key={}; the job is no longer active: {}",
                    job.getType(),
                    job.getKey(),
                    failException.getMessage()
            );
        }
    }

    static boolean isJobNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ProblemException problemException
                    && Integer.valueOf(404).equals(problemException.details().getStatus())
                    && "NOT_FOUND".equals(problemException.details().getTitle())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
