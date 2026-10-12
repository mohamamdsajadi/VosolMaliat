package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="execution_paper_state")
public class ExecutionPaperStateDefinition extends LookupDefinition {
    protected ExecutionPaperStateDefinition() {}
    public ExecutionPaperStateDefinition(ExecutionPaperState value) { super(value); }
}
