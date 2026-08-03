package com.mparsict.vosolmaliat.unit.bpmn;

import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetLastDebtStatusBpmnContractUnitTest {

    private static final String ZEEBE = "http://camunda.org/schema/zeebe/1.0";
    private static final Path BPMN_FILE = Path.of("src/main/resources/bpmn/GetLastDebtStatus.bpmn");
    private final Document document = readBpmn();

    @Test
    void processUsesImplementedWorkersAndCorrelatedMessage() {
        assertThat(elementById(ProcessConstants.GET_LAST_DEBT_STATUS_PROCESS_ID)
                .getAttribute("isExecutable")).isEqualTo("true");
        assertThat(taskType("Activity_1khsio8")).isEqualTo(ProcessConstants.JOB_GET_DEBT_FROM_SANIM);
        assertThat(taskType("Activity_0d55kj2"))
                .isEqualTo(ProcessConstants.JOB_UPDATE_TAXPAYER_PAYMENT_STATUS);
        assertThat(elementById("Message_3lah0dr").getAttribute("name"))
                .isEqualTo(ProcessConstants.MESSAGE_TAXPAYER_DEBT);
        Element subscription = firstChild(elementById("Message_3lah0dr"), "subscription");
        assertThat(subscription.getAttribute("correlationKey"))
                .isEqualTo("=" + ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID);
    }

    @Test
    void workerAndMessageMappingsExposeCallableProcessContract() {
        assertThat(inputMappings("Activity_1khsio8"))
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID,
                        "=" + ProcessConstants.VAR_EXECUTION_PAPER_ID);
        assertThat(outputMappings("Activity_1khsio8"))
                .containsEntry(ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID,
                        "=" + ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID);
        assertThat(outputMappings("Event_0f49589"))
                .containsEntry(ProcessConstants.VAR_SANIM_DEBT_STATUS,
                        "=" + ProcessConstants.VAR_SANIM_DEBT_STATUS);
        assertThat(inputMappings("Activity_0d55kj2"))
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID,
                        "=" + ProcessConstants.VAR_EXECUTION_PAPER_ID)
                .containsEntry(ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID,
                        "=" + ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID)
                .containsEntry(ProcessConstants.VAR_SANIM_DEBT_STATUS,
                        "=" + ProcessConstants.VAR_SANIM_DEBT_STATUS);
        assertThat(outputMappings("Activity_0d55kj2"))
                .containsEntry(ProcessConstants.VAR_DEBT_STATUS_UPDATED,
                        "=" + ProcessConstants.VAR_DEBT_STATUS_UPDATED);
    }

    private String taskType(String id) {
        return firstChild(elementById(id), "taskDefinition").getAttribute("type");
    }

    private Map<String, String> inputMappings(String id) {
        return mappings(id, "input");
    }

    private Map<String, String> outputMappings(String id) {
        return mappings(id, "output");
    }

    private Map<String, String> mappings(String id, String localName) {
        Map<String, String> result = new HashMap<>();
        NodeList nodes = elementById(id).getElementsByTagNameNS(ZEEBE, localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element mapping = (Element) nodes.item(i);
            result.put(mapping.getAttribute("target"), mapping.getAttribute("source"));
        }
        return result;
    }

    private Element elementById(String id) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        throw new AssertionError("BPMN element not found: " + id);
    }

    private Element firstChild(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS(ZEEBE, localName);
        assertThat(children.getLength()).isGreaterThan(0);
        return (Element) children.item(0);
    }

    private static Document readBpmn() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(BPMN_FILE.toFile());
        } catch (Exception ex) {
            throw new AssertionError("Could not read BPMN", ex);
        }
    }
}
