package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Securely exposes XML leaf values without assuming an external XML schema. */
@Component
public class DebtPaperXmlReader {

    public Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception exception) {
            throw new DebtPaperPushException(
                    HttpStatus.BAD_REQUEST,
                    -7,
                    "Debt Paper XML is not well-formed.",
                    0L,
                    exception
            );
        }
    }

    public Map<String, String> read(String xml) {
        Map<String, String> values = new LinkedHashMap<>();
        collect(parse(xml).getDocumentElement(), "", values);
        return values;
    }

    private void collect(Element element, String parentPath, Map<String, String> values) {
        String name = element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
        String path = parentPath.isBlank() ? name : parentPath + "." + name;
        NodeList children = element.getChildNodes();
        boolean hasElementChild = false;
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                hasElementChild = true;
                collect(childElement, path, values);
            }
        }
        if (!hasElementChild) {
            String value = element.getTextContent() == null ? null : element.getTextContent().trim();
            if (value != null && !value.isBlank()) {
                values.putIfAbsent(normalize(name), value);
                values.putIfAbsent(normalize(path), value);
            }
        }
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
