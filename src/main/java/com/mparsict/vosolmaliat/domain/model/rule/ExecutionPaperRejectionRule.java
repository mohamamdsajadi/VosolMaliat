package com.mparsict.vosolmaliat.domain.model.rule;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Scope-specific API adapter; both scopes use the documented dynamic_control_rule table. */
@Entity @DiscriminatorValue("ISSUANCE")
@SuperBuilder @NoArgsConstructor
public class ExecutionPaperRejectionRule extends DynamicControlRule {}
