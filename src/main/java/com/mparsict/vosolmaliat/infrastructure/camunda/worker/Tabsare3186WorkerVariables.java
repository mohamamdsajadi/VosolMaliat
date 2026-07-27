package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;

import java.util.List;
import java.util.Map;

final class Tabsare3186WorkerVariables {

    private Tabsare3186WorkerVariables() {
    }

    static String requiredString(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing required process variable: " + name);
        }
        return String.valueOf(value).trim();
    }

    static String requiredStringOrNested(
            Map<String, Object> variables,
            String name,
            String rootName,
            String... path) {
        Object directValue = variables.get(name);
        if (directValue != null && !String.valueOf(directValue).isBlank()) {
            return String.valueOf(directValue).trim();
        }

        Object nestedValue = variables.get(rootName);
        for (String segment : path) {
            if (!(nestedValue instanceof Map<?, ?> map)) {
                nestedValue = null;
                break;
            }
            nestedValue = map.get(segment);
        }
        if (nestedValue == null || String.valueOf(nestedValue).isBlank()) {
            throw new IllegalArgumentException("Missing required process variable: " + name);
        }
        return String.valueOf(nestedValue).trim();
    }

    static Long requiredLong(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required process variable: " + name);
        }
        return value instanceof Number number
                ? number.longValue()
                : Long.valueOf(String.valueOf(value));
    }

    static List<CompanyRegistryRelatedPersonData> requiredRelatedPersons(
            ObjectMapper objectMapper,
            Map<String, Object> variables,
            String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required process variable: " + name);
        }
        return objectMapper.convertValue(
                value,
                new TypeReference<List<CompanyRegistryRelatedPersonData>>() {
                }
        );
    }

    static String normalizeJalaliDate(String value) {
        String normalized = requiredDate(value)
                .replace('۰', '0')
                .replace('۱', '1')
                .replace('۲', '2')
                .replace('۳', '3')
                .replace('۴', '4')
                .replace('۵', '5')
                .replace('۶', '6')
                .replace('۷', '7')
                .replace('۸', '8')
                .replace('۹', '9')
                .replace('/', '-');
        return normalized.length() > 10 ? normalized.substring(0, 10) : normalized;
    }

    static boolean activeAt(CompanyRegistryRelatedPersonData person, String debtPaperDate) {
        String activeDate = normalizeJalaliDate(debtPaperDate);
        String startDate = optionalDate(person.startDate());
        String endDate = optionalDate(person.endDate());
        return (startDate == null || startDate.compareTo(activeDate) <= 0)
                && (endDate == null || endDate.compareTo(activeDate) >= 0);
    }

    private static String optionalDate(String value) {
        return value == null || value.isBlank() ? null : normalizeJalaliDate(value);
    }

    private static String requiredDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Jalali date is required");
        }
        return value.trim();
    }
}
