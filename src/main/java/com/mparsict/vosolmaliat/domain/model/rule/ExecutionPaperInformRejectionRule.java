package com.mparsict.vosolmaliat.domain.model.rule;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Scope-specific API adapter; both scopes use the documented dynamic_control_rule table. */
@Entity @DiscriminatorValue("INFORM")
@SuperBuilder @NoArgsConstructor
public class ExecutionPaperInformRejectionRule extends DynamicControlRule {}
