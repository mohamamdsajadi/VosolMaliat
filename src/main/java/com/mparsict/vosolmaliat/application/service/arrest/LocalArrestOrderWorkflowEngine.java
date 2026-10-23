package com.mparsict.vosolmaliat.application.service.arrest;

import com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage;
import com.mparsict.vosolmaliat.domain.model.execution.ArrestOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;
import static com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage.*;

@Component
@ConditionalOnProperty(name = "app.arrest-order.engine", havingValue = "local", matchIfMissing = true)
public class LocalArrestOrderWorkflowEngine implements ArrestOrderWorkflowEngine {
    public String name() { return "LOCAL"; }
    public void advance(ArrestOrder order, Action action) {
        ArrestOrderStage expected = switch (action) {
            case START, REFER_SPECIAL -> null;
            case PREPARE -> SPECIAL_DEPARTMENT_REVIEW;
            case APPROVE, APPROVE_TO_OFFICER -> AWAITING_APPROVAL;
            case ASSIGN_OFFICER -> AWAITING_OFFICER_ASSIGNMENT;
        };
        if (order.getStage() != expected) throw new ResponseStatusException(CONFLICT, "مرحله کار تغییر کرده است؛ صفحه را بروزرسانی کنید.");
        order.setStage(switch (action) {
            case START, PREPARE -> AWAITING_APPROVAL;
            case REFER_SPECIAL -> SPECIAL_DEPARTMENT_REVIEW;
            case APPROVE -> AWAITING_OFFICER_ASSIGNMENT;
            case ASSIGN_OFFICER, APPROVE_TO_OFFICER -> ASSIGNED_TO_OFFICER;
        });
    }
}
