package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryNaturalPersonData;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FakeCompanyRegistryService {

    private static final String[] FIRST_NAMES = {"علی", "مریم", "رضا", "زهرا", "حسین", "سارا"};
    private static final String[] LAST_NAMES = {"احمدی", "محمدی", "رضایی", "کریمی", "حسینی", "مرادی"};

    public List<CompanyRegistryRelatedPersonData> fetchRelatedPersons(String ownerNationalId) {
        String normalizedNationalId = requireNationalId(ownerNationalId);
        long seed = Integer.toUnsignedLong(normalizedNationalId.hashCode());
        String firstNationalCode = tenDigits(seed * 31 + 11);
        String secondNationalCode = tenDigits(seed * 37 + 19);
        String relatedLegalNationalId = elevenDigits(seed * 41 + 23);
        int firstIndex = (int) (seed % FIRST_NAMES.length);
        int secondIndex = (firstIndex + 2) % FIRST_NAMES.length;

        return List.of(
                new CompanyRegistryRelatedPersonData(
                        normalizedNationalId + "-N1-CEO",
                        RelatedPersonType.NATURAL,
                        RelatedPersonPostCode.CEO,
                        "1400-01-01",
                        null,
                        true,
                        firstNationalCode,
                        null,
                        FIRST_NAMES[firstIndex],
                        LAST_NAMES[firstIndex],
                        "محمد",
                        "1365-04-12",
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new CompanyRegistryRelatedPersonData(
                        normalizedNationalId + "-N1-BOARD",
                        RelatedPersonType.NATURAL,
                        RelatedPersonPostCode.BOARD_MEMBER,
                        "1399-06-15",
                        null,
                        true,
                        firstNationalCode,
                        null,
                        FIRST_NAMES[firstIndex],
                        LAST_NAMES[firstIndex],
                        "محمد",
                        "1365-04-12",
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new CompanyRegistryRelatedPersonData(
                        normalizedNationalId + "-N2-CHAIR",
                        RelatedPersonType.NATURAL,
                        RelatedPersonPostCode.BOARD_CHAIR,
                        "1401-02-01",
                        null,
                        false,
                        secondNationalCode,
                        null,
                        FIRST_NAMES[secondIndex],
                        LAST_NAMES[secondIndex],
                        "حسن",
                        "1372-09-20",
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new CompanyRegistryRelatedPersonData(
                        normalizedNationalId + "-L1-SHAREHOLDER",
                        RelatedPersonType.LEGAL,
                        RelatedPersonPostCode.SHAREHOLDER,
                        "1398-01-01",
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        relatedLegalNationalId,
                        "ث-" + lastSixDigits(seed),
                        "1390-03-10",
                        "شرکت سرمایه‌گذاری نمونه " + lastFourDigits(seed),
                        new CompanyRegistryNaturalPersonData(
                                firstNationalCode,
                                null,
                                FIRST_NAMES[firstIndex],
                                LAST_NAMES[firstIndex],
                                "محمد",
                                "1365-04-12"
                        )
                )
        );
    }

    private String requireNationalId(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    "LEGAL_PERSON_NATIONAL_ID_REQUIRED",
                    "شناسه ملی شخص حقوقی برای استعلام ثبت شرکت‌ها الزامی است"
            );
        }
        return value.trim();
    }

    private String tenDigits(long value) {
        return String.format("%010d", Math.floorMod(value, 10_000_000_000L));
    }

    private String elevenDigits(long value) {
        return String.format("%011d", Math.floorMod(value, 100_000_000_000L));
    }

    private String lastSixDigits(long value) {
        return String.format("%06d", Math.floorMod(value, 1_000_000L));
    }

    private String lastFourDigits(long value) {
        return String.format("%04d", Math.floorMod(value, 10_000L));
    }
}
