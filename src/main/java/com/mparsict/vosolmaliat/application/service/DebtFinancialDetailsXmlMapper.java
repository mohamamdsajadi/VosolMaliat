package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Preserves the source monetary elements as XML while assigning them to the
 * document-model detail columns according to the Persian data dictionary.
 * Repeated elements are retained in source order.
 */
final class DebtFinancialDetailsXmlMapper {
    private enum Category { LIABILITY, CREDIT, PENALTY, PAYMENT, REMISSION, OUTSTANDING }

    private static final Set<String> EF03_LIABILITY = Set.of("CB");
    private static final Set<String> EF03_PENALTY = Set.of("GB", "KB", "MB", "NB");
    private static final Set<String> EF03_PAYMENT = Set.of("EB");
    private static final Set<String> EF03_REMISSION = Set.of("DB");
    private static final Set<String> EF03_OUTSTANDING = Set.of("FB", "HB");

    private static final Set<String> VAT_LIABILITY = Set.of(
            "DB", "EB", "FB", "GB", "HB", "IB", "JB", "KB", "LB", "MB",
            "NB", "OB", "ZB", "AC", "QC", "RC", "SC", "ED", "PD", "QD");
    private static final Set<String> VAT_CREDIT = Set.of(
            "PB", "QB", "RB", "SB", "TB", "UB", "VB", "WB", "XB", "YB", "BC", "CC");
    private static final Set<String> VAT_PENALTY = Set.of(
            "DC", "EC", "FC", "GC", "TC", "UC", "VC", "WC", "FD", "GD", "HD");
    private static final Set<String> VAT_PAYMENT = Set.of("JC", "KC", "ZC", "AD", "ID");
    private static final Set<String> VAT_OUTSTANDING = Set.of("LC", "MC", "BD", "CD", "JD");

    private static final Set<String> VAT2_LIABILITY = Set.of(
            "QC", "RC", "SC", "ED", "QE", "RE", "SE", "TE", "UE", "VE");
    private static final Set<String> VAT2_PENALTY = Set.of(
            "TC", "UC", "VC", "WC", "FD", "GD", "HD", "IE", "JE", "KE", "LE", "ME", "NE");
    private static final Set<String> VAT2_PAYMENT = Set.of("ZC", "AD", "ID", "OE");
    private static final Set<String> VAT2_OUTSTANDING = Set.of("BD", "CD", "JD", "GE", "PE");

    private DebtFinancialDetailsXmlMapper() {}

    static DebtFinancialDetails map(PaperType paperType, Document source) {
        findMain(source); // Validate the document contract before collecting nested sections.
        EnumMap<Category, Document> fragments = new EnumMap<>(Category.class);
        Map<String, Integer> occurrences = new HashMap<>();
        collect(paperType, source.getDocumentElement(), fragments, occurrences);
        return new DebtFinancialDetails(
                xml(fragments.get(Category.LIABILITY)),
                xml(fragments.get(Category.CREDIT)),
                xml(fragments.get(Category.PENALTY)),
                xml(fragments.get(Category.PAYMENT)),
                xml(fragments.get(Category.REMISSION)),
                xml(fragments.get(Category.OUTSTANDING)));
    }

    private static void collect(PaperType paperType, Element parent,
            EnumMap<Category, Document> fragments, Map<String, Integer> occurrences) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element element)) continue;
            String tag = localName(element);
            int occurrence = occurrences.merge(tag, 1, Integer::sum);
            Category category = category(paperType, tag, occurrence);
            if (category != null) {
                Document fragment = fragments.computeIfAbsent(category, key -> newFragment(paperType, key));
                fragment.getDocumentElement().appendChild(fragment.importNode(element, true));
            } else {
                collect(paperType, element, fragments, occurrences);
            }
        }
    }

    private static Category category(PaperType type, String tag, int occurrence) {
        if (type == PaperType.EF03) {
            if (EF03_LIABILITY.contains(tag)) return Category.LIABILITY;
            if (EF03_PENALTY.contains(tag)) return Category.PENALTY;
            if (EF03_PAYMENT.contains(tag)) return Category.PAYMENT;
            if (EF03_REMISSION.contains(tag)) return Category.REMISSION;
            if (EF03_OUTSTANDING.contains(tag)) return Category.OUTSTANDING;
            return null;
        }
        if (type == PaperType.EF23 || type == PaperType.EF23_1) {
            // RD/SD are reused by the source format. The first occurrence is a
            // purchase adjustment; a later occurrence is waste payment/balance.
            if ("RD".equals(tag)) return occurrence == 1 ? Category.CREDIT : Category.PAYMENT;
            if ("SD".equals(tag)) return occurrence == 1 ? Category.CREDIT : Category.OUTSTANDING;
            if (VAT_LIABILITY.contains(tag)) return Category.LIABILITY;
            if (VAT_CREDIT.contains(tag)) return Category.CREDIT;
            if (VAT_PENALTY.contains(tag)) return Category.PENALTY;
            if (VAT_PAYMENT.contains(tag)) return Category.PAYMENT;
            if (VAT_OUTSTANDING.contains(tag)) return Category.OUTSTANDING;
            return null;
        }
        if (type == PaperType.EF23_2) {
            if (VAT2_LIABILITY.contains(tag)) return Category.LIABILITY;
            if (VAT2_PENALTY.contains(tag)) return Category.PENALTY;
            if (VAT2_PAYMENT.contains(tag)) return Category.PAYMENT;
            if (VAT2_OUTSTANDING.contains(tag)) return Category.OUTSTANDING;
        }
        return null;
    }

    private static Element findMain(Document document) {
        for (Node node : iterable(document.getElementsByTagNameNS("*", "MAIN"))) {
            if (node instanceof Element element) return element;
        }
        throw SanimXmlValues.invalid("XML MAIN section is missing.");
    }

    private static Iterable<Node> iterable(org.w3c.dom.NodeList nodes) {
        return () -> new java.util.Iterator<>() {
            private int index;
            public boolean hasNext() { return index < nodes.getLength(); }
            public Node next() { return nodes.item(index++); }
        };
    }

    private static Document newFragment(PaperType paperType, Category category) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement("details");
            root.setAttribute("paperType", paperType.name());
            root.setAttribute("category", category.name());
            document.appendChild(root);
            return document;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create debt-detail XML", exception);
        }
    }

    private static String xml(Document document) {
        if (document == null) return "<details/>";
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(document.getDocumentElement()), new StreamResult(output));
            return output.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize debt-detail XML", exception);
        }
    }

    private static String localName(Element element) {
        return element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
    }
}
