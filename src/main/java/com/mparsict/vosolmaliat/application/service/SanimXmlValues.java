package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import org.springframework.http.HttpStatus;
import org.w3c.dom.*;
import java.math.BigDecimal;
import java.util.*;

/** Occurrence-aware view; source XML itself remains unchanged in the receipt. */
final class SanimXmlValues {
    private final Element main;
    SanimXmlValues(Document document) {
        List<Element> mains = new ArrayList<>();
        collect(document.getDocumentElement(), mains);
        if (mains.size() != 1) throw invalid("XML must contain exactly one MAIN section.");
        main = mains.getFirst();
    }
    private void collect(Element node, List<Element> mains) {
        String name = node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
        if (name.equals("MAIN")) mains.add(node);
        for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element e) collect(e, mains);
    }
    private List<String> values(Element parent, String tag) {
        List<String> values = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && tag.equals(e.getLocalName() == null ? e.getNodeName() : e.getLocalName())) {
                for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling())
                    if (child instanceof Element) throw invalid("Expected scalar value for " + tag);
                values.add(e.getTextContent().trim());
            }
        }
        return values;
    }
    String text(String tag) {
        List<String> v = values(main, tag);
        if (v.isEmpty()) return null;
        if (new HashSet<>(v).size() > 1) throw invalid("Conflicting repeated tag: " + tag);
        return v.getFirst().isBlank() ? null : v.getFirst();
    }
    String required(String tag) {
        String value = text(tag);
        if (value == null) throw invalid("Required XML tag is missing: " + tag);
        return value;
    }
    BigDecimal amount(String tag) {
        List<String> v = values(main, tag);
        // EF03 receipt contract: an absent monetary tag means zero.
        if (v.isEmpty()) return BigDecimal.ZERO;
        if (v.size() > 1) throw invalid("Ambiguous aggregate tag: " + tag);
        return decimal(v.getFirst(), tag);
    }
    BigDecimal wasteAmount(String tag) {
        List<String> v = values(main, tag);
        if (v.isEmpty() || v.stream().allMatch(x -> decimal(x, tag).signum() == 0)) return BigDecimal.ZERO;
        throw invalid("Cannot distinguish waste from goods/services aggregate tag " + tag);
    }
    BigDecimal sum(String... tags) {
        BigDecimal result = BigDecimal.ZERO;
        for (String tag : tags) result = result.add(amount(tag));
        return result;
    }
    static String digits(String value) {
        StringBuilder s = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= '۰' && c <= '۹') s.append((char)('0' + c - '۰'));
            else if (c >= '٠' && c <= '٩') s.append((char)('0' + c - '٠'));
            else s.append(c);
        }
        return s.toString();
    }
    static BigDecimal decimal(String raw, String tag) {
        try {
            String n = digits(raw).replace("٬", ",").replace('٫', '.').trim();
            if (!n.matches("[+-]?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]+)?"))
                throw new NumberFormatException();
            return new BigDecimal(n.replace(",", ""));
        } catch (RuntimeException e) { throw invalid("Invalid numeric value in " + tag); }
    }
    static DebtPaperPushException invalid(String message) {
        return new DebtPaperPushException(HttpStatus.UNPROCESSABLE_ENTITY, -7, message);
    }
}
