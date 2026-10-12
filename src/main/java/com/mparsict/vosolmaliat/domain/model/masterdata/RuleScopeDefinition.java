package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.RuleScope;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="rule_scope")
public class RuleScopeDefinition extends LookupDefinition {
    protected RuleScopeDefinition() {}
    public RuleScopeDefinition(RuleScope value) { super(value); }
}
