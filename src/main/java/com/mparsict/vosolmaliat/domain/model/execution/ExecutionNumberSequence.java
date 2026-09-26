package com.mparsict.vosolmaliat.domain.model.execution;
import jakarta.persistence.*;
@Entity @Table(name = "execution_number_sequence")
public class ExecutionNumberSequence {
    @Id private Integer year;
    @Column(name = "last_number", nullable = false) private Integer lastNumber;
}
