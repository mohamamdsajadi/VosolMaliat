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

class ExecutionPaperBpmnContractUnitTest {
    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String ZEEBE = "http://camunda.org/schema/zeebe/1.0";
    private final Document document = readBpmn(Path.of("src/main/resources/bpmn/ExecutionPapers.bpmn"));

    @Test
    void processContainsOnlyRuleDecisionAndIssuanceOrRejection() {
        assertThat(elementById(ProcessConstants.PROCESS_ID).getAttribute("isExecutable")).isEqualTo("true");
        assertThat(zeebeTaskType("Activity_1ahtl92")).isEqualTo(ProcessConstants.JOB_CHECK_ISSUANCE);
        assertThat(zeebeTaskType("Activity_0rwz3ak")).isEqualTo(ProcessConstants.JOB_ISSUE_PAPER);
        assertThat(zeebeTaskType("Activity_010iyzn")).isEqualTo(ProcessConstants.JOB_SEND_REJECTION);
        assertThat(sequence("Flow_05448p9")).containsEntry("sourceRef", "Activity_0rwz3ak")
                .containsEntry("targetRef", "Event_10ewku3");
        assertThat(sequence("Flow_05tcjnl")).containsEntry("sourceRef", "Activity_010iyzn")
                .containsEntry("targetRef", "Event_10ewku3");
    }

    @Test
    void ruleTaskPublishesDecisionAndStructuredRejection() {
        assertThat(outputMappings("Activity_1ahtl92"))
                .containsEntry(ProcessConstants.VAR_CAN_ISSUE, "=" + ProcessConstants.VAR_CAN_ISSUE)
                .containsEntry(ProcessConstants.VAR_REJECTION_CODE, "=" + ProcessConstants.VAR_REJECTION_CODE)
                .containsEntry(ProcessConstants.VAR_REJECTION_DESC, "=" + ProcessConstants.VAR_REJECTION_DESC)
                .containsEntry(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE,
                        "=" + ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE);
    }

    @Test
    void issuanceTaskPublishesResponseFields() {
        assertThat(outputMappings("Activity_0rwz3ak"))
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, "=" + ProcessConstants.VAR_EXECUTION_PAPER_ID)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_CODE, "=" + ProcessConstants.VAR_EXECUTION_PAPER_CODE)
                .containsEntry(ProcessConstants.VAR_SUCCESS, "=" + ProcessConstants.VAR_SUCCESS);
    }

    @Test
    void gatewayUsesRuleDecision() {
        assertThat(condition("Flow_0eioah5")).isEqualTo("=" + ProcessConstants.VAR_CAN_ISSUE);
        assertThat(condition("Flow_0k7613y")).isEqualTo("=not(" + ProcessConstants.VAR_CAN_ISSUE + ")");
    }

    private String zeebeTaskType(String id) {
        return firstChild(elementById(id), ZEEBE, "taskDefinition").getAttribute("type");
    }

    private Map<String, String> outputMappings(String id) {
        NodeList outputs = elementById(id).getElementsByTagNameNS(ZEEBE, "output");
        Map<String, String> result = new HashMap<>();
        for (int index = 0; index < outputs.getLength(); index++) {
            Element output = (Element) outputs.item(index);
            result.put(output.getAttribute("target"), output.getAttribute("source"));
        }
        return result;
    }

    private Map<String, String> sequence(String id) {
        Element flow = elementById(id);
        return Map.of("sourceRef", flow.getAttribute("sourceRef"), "targetRef", flow.getAttribute("targetRef"));
    }

    private String condition(String id) {
        return firstChild(elementById(id), BPMN, "conditionExpression").getTextContent().trim();
    }

    private Element elementById(String id) {
        NodeList all = document.getElementsByTagName("*");
        for (int index = 0; index < all.getLength(); index++) {
            Node node = all.item(index);
            if (node instanceof Element element && id.equals(element.getAttribute("id"))) return element;
        }
        throw new AssertionError("BPMN element not found: " + id);
    }

    private Element firstChild(Element parent, String namespace, String name) {
        NodeList children = parent.getElementsByTagNameNS(namespace, name);
        assertThat(children.getLength()).isGreaterThan(0);
        return (Element) children.item(0);
    }

    private static Document readBpmn(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(path.toFile());
        } catch (Exception exception) {
            throw new AssertionError("Could not read BPMN file", exception);
        }
    }
}
