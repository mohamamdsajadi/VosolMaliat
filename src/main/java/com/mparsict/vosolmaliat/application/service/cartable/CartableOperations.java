package com.mparsict.vosolmaliat.application.service.cartable;

import com.mparsict.vosolmaliat.application.dto.request.cartable.*;
import com.mparsict.vosolmaliat.application.dto.response.cartable.CartableTaskResponse;
import com.mparsict.vosolmaliat.application.dto.response.cartable.DelegationTargetResponse;
import java.util.List;

public interface CartableOperations {
    List<CartableTaskResponse> myTasks(CartableTaskSearchCriteria criteria);
    List<CartableTaskResponse> candidateTasks();
    CartableTaskResponse getTask(Long taskKey);
    CartableTaskResponse claim(Long taskKey, ClaimTaskRequest request);
    CartableTaskResponse unclaim(Long taskKey);
    void complete(Long taskKey, CompleteTaskRequest request);
    CartableTaskResponse assign(Long taskKey, AssignTaskRequest request);
    List<DelegationTargetResponse> delegationTargets(Long taskKey);
    void delegate(Long taskKey, DelegateTaskRequest request);
}
