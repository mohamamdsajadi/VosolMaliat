package com.mparsict.vosolmaliat.application.service.arrest;

import com.mparsict.vosolmaliat.domain.model.execution.ArrestOrder;

/** Replace this adapter when Camunda is restored; business data and letter snapshots stay local. */
public interface ArrestOrderWorkflowEngine {
    enum Action { START, REFER_SPECIAL, PREPARE, APPROVE, APPROVE_TO_OFFICER, ASSIGN_OFFICER }
    String name();
    void advance(ArrestOrder order, Action action);
}
