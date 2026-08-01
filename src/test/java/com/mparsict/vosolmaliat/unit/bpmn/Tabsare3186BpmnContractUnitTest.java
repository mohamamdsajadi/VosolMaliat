package com.mparsict.vosolmaliat.unit.bpmn;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Tabsare3186BpmnContractUnitTest {

    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String ZEEBE = "http://camunda.org/schema/zeebe/1.0";
    private final Document document = readBpmn();

    @Test
    void executableStepsExposeExpectedJobTypes() {
        assertThat(taskType("Activity_081d0tt"))
                .isEqualTo("send-official-gazette-board-members-request");
        assertThat(taskType("Activity_1f4jd51"))
                .isEqualTo("filter-related-persons-by-debt-paper-date");
        assertThat(taskType("Activity_0lrql5n"))
                .isEqualTo("send-related-persons-for-restrictions");
        assertThat(taskType("Activity_0l82eat"))
                .isEqualTo("store-related-persons");
    }

    @Test
    void officialGazetteResponseUsesRequestIdCorrelation() {
        Element event = elementById("Event_0nuru68");
        Element definition = firstChild(event, BPMN, "messageEventDefinition");
        assertThat(definition.getAttribute("messageRef"))
                .isEqualTo("Message_official_gazette_related_persons");

        Element message = elementById("Message_official_gazette_related_persons");
        assertThat(message.getAttribute("name"))
                .isEqualTo("official_gazette_related_persons_received");
        assertThat(firstChild(message, ZEEBE, "subscription").getAttribute("correlationKey"))
                .isEqualTo("=officialGazetteRequestId");
    }

    private String taskType(String id) {
        return firstChild(elementById(id), ZEEBE, "taskDefinition").getAttribute("type");
    }

    private Element elementById(String id) {
        var elements = document.getElementsByTagNameNS("*", "*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        throw new AssertionError("BPMN element not found: " + id);
    }

    private Element firstChild(Element parent, String namespace, String localName) {
        var children = parent.getElementsByTagNameNS(namespace, localName);
        if (children.getLength() == 0) {
            throw new AssertionError("Child element not found: " + localName);
        }
        return (Element) children.item(0);
    }

    private Document readBpmn() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(
                    Path.of("src/main/resources/bpmn/Tabsare3-186 (1).bpmn").toFile()
            );
        } catch (Exception ex) {
            throw new AssertionError("Could not read Tabsare3-186 BPMN", ex);
        }
    }
}
