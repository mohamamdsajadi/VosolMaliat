package com.mparsict.vosolmaliat.domain.model.sanim;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
/** TEXT avoids SQLite NUMERIC affinity silently converting large decimals to floating point. */
@Converter
public class ExactDecimalConverter implements AttributeConverter<BigDecimal, String> {
    public String convertToDatabaseColumn(BigDecimal value) { return value == null ? null : value.toPlainString(); }
    public BigDecimal convertToEntityAttribute(String value) { return value == null ? null : new BigDecimal(value); }
}
