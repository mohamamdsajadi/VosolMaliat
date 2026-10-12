package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.*;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public final class EnumLookupConverters {
    private EnumLookupConverters() {}

    private abstract static class Base<E extends Enum<E> & LookupValue> implements AttributeConverter<E,Integer> {
        private final Class<E> type;
        Base(Class<E> type) { this.type=type; }
        @Override public Integer convertToDatabaseColumn(E value) { return value == null ? null : value.id(); }
        @Override public E convertToEntityAttribute(Integer id) {
            if (id == null) return null;
            for (E value : type.getEnumConstants()) if (value.id() == id) return value;
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " lookup id: " + id);
        }
    }

    @Converter public static class FinalizationReasonConverter extends Base<FinalizationReason> { public FinalizationReasonConverter(){super(FinalizationReason.class);} }
    @Converter public static class TaxSourceConverter extends Base<TaxSourceType> { public TaxSourceConverter(){super(TaxSourceType.class);} }
    @Converter public static class TaxPeriodConverter extends Base<TaxPeriod> { public TaxPeriodConverter(){super(TaxPeriod.class);} }
    @Converter public static class TaxpayerTypeConverter extends Base<TaxpayerType> { public TaxpayerTypeConverter(){super(TaxpayerType.class);} }
    @Converter public static class RestrictionStateConverter extends Base<RestrictionState> { public RestrictionStateConverter(){super(RestrictionState.class);} }
    @Converter public static class RuleScopeConverter extends Base<RuleScope> { public RuleScopeConverter(){super(RuleScope.class);} }
    @Converter public static class PaperTypeConverter extends Base<PaperType> { public PaperTypeConverter(){super(PaperType.class);} }
}
