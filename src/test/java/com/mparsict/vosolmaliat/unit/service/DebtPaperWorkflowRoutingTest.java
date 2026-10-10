package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DebtPaperWorkflowRoutingTest {
    @Test void commitsReceiptAndRequestBeforeStartingCamundaAndReturnsItsId() {
        var mapper = mock(DebtPaperPushMapper.class);
        var persistence = mock(DebtPaperPushPersistenceService.class);
        var camunda = mock(CamundaProcessService.class);
        var service = new DebtPaperPushService(mapper, persistence, camunda);
        var source = mock(DebtPaperPushRequest.class);
        var mapped = mock(DebtPaperIssuanceRequest.class);
        when(source.announcementType()).thenReturn("LTR_EF03");
        when(persistence.receive(source, "trace")).thenReturn(1L);
        when(mapper.map(source)).thenReturn(mapped);
        when(persistence.prepareWorkflow(1L, mapped)).thenReturn(2L);
        when(camunda.startExecutionPaperIssuanceProcess(2L, mapped)).thenReturn(
                new IssuanceResponse(true, 3L, "14050000001", null, null, null, "issued"));
        assertThat(service.push(source, "trace").executionPaperId()).isEqualTo(3L);
        var order = inOrder(persistence, camunda);
        order.verify(persistence).receive(source, "trace");
        order.verify(persistence).prepareWorkflow(1L, mapped);
        order.verify(camunda).startExecutionPaperIssuanceProcess(2L, mapped);
        order.verify(persistence).markSuccessful(1L, 3L);
    }

    @Test void mappingFailureNeverStartsAnInstance() {
        var mapper = mock(DebtPaperPushMapper.class);
        var persistence = mock(DebtPaperPushPersistenceService.class);
        var camunda = mock(CamundaProcessService.class);
        var service = new DebtPaperPushService(mapper, persistence, camunda);
        var source = mock(DebtPaperPushRequest.class);
        when(source.announcementType()).thenReturn("EF03");
        when(persistence.receive(source, "trace")).thenReturn(1L);
        when(mapper.map(source)).thenThrow(new IllegalArgumentException("invalid XML"));
        assertThatThrownBy(() -> service.push(source, "trace")).hasMessage("invalid XML");
        verify(persistence).markNeedsReview(1L, "invalid XML");
        verifyNoInteractions(camunda);
    }
}
