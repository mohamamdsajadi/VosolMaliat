package com.mparsict.vosolmaliat.unit.bpmn;

import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionPaperBpmnContractUnitTest {

    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String ZEEBE = "http://camunda.org/schema/zeebe/1.0";
    private static final Path EXECUTION_PAPERS_BPMN = Path.of("src/main/resources/bpmn/ExecutionPapers.bpmn");

    private final Document document = readBpmn(EXECUTION_PAPERS_BPMN);

    @Test
    void executionPaperProcessUsesExpectedCamunda8WorkerTypes() {
        Element process = elementById("Process_0rtbdc5");

        assertThat(process.getAttribute("id")).isEqualTo(ProcessConstants.PROCESS_ID);
        assertThat(process.getAttribute("isExecutable")).isEqualTo("true");
        assertThat(zeebeTaskType("Activity_1ahtl92")).isEqualTo(ProcessConstants.JOB_CHECK_ISSUANCE);
        assertThat(zeebeTaskType("Activity_0rwz3ak")).isEqualTo(ProcessConstants.JOB_ISSUE_PAPER);
        assertThat(zeebeTaskType("Activity_010iyzn")).isEqualTo(ProcessConstants.JOB_SEND_REJECTION);
        assertThat(zeebeTaskType("Activity_1f7l5yj"))
                .isEqualTo(ProcessConstants.JOB_SEND_EXECUTION_PAPER_ID_TO_SANIM);
    }

    @Test
    void issuedExecutionPaperStartsInformSubprocessAndOperationalCartableInParallel() {
        assertThat(sequenceTarget("Flow_0pf5o1c")).isEqualTo("Gateway_0fw9zja");
        assertThat(sequenceTarget("Flow_11dgpnv")).isEqualTo("Activity_0xodqlk");
        assertThat(sequenceTarget("Flow_07fwfdh")).isEqualTo("Activity_0generalHeadReview");

        Element calledElement = firstChild(elementById("Activity_0xodqlk"), ZEEBE, "calledElement");
        assertThat(calledElement.getAttribute("processId")).isEqualTo("Process_0lt64q4");
        assertThat(calledElement.getAttribute("propagateAllChildVariables")).isEqualTo("true");
    }

    @Test
    void operationalUserTasksFollowTheBusinessRoleDelegationHierarchy() {
        Map<String, String> expectedCandidateGroups = Map.of(
                "Activity_0generalHeadReview", ProcessConstants.VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS,
                "Activity_0taxOfficeHeadReview", ProcessConstants.VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS,
                "Activity_0taxUnitHeadReview", ProcessConstants.VAR_TAX_UNIT_HEAD_CANDIDATE_GROUPS,
                "Activity_0taxExpertProcess", ProcessConstants.VAR_TAX_EXPERT_CANDIDATE_GROUPS
        );

        expectedCandidateGroups.forEach((taskId, variable) ->
                assertThat(candidateGroups(taskId)).isEqualTo("=" + variable));

        assertThat(sequenceSourceAndTarget("Flow_0generalHeadDone"))
                .containsEntry("sourceRef", "Activity_0generalHeadReview")
                .containsEntry("targetRef", "Activity_0taxOfficeHeadReview");
        assertThat(sequenceSourceAndTarget("Flow_0taxOfficeHeadDone"))
                .containsEntry("sourceRef", "Activity_0taxOfficeHeadReview")
                .containsEntry("targetRef", "Activity_0taxUnitHeadReview");
        assertThat(sequenceSourceAndTarget("Flow_0taxUnitHeadDone"))
                .containsEntry("sourceRef", "Activity_0taxUnitHeadReview")
                .containsEntry("targetRef", "Activity_0taxExpertProcess");
    }

    @Test
    void issuanceGatewayConditionsUseTheWorkerOutputVariable() {
        assertThat(conditionExpression("Flow_0eioah5")).isEqualTo("=" + ProcessConstants.VAR_CAN_ISSUE);
        assertThat(conditionExpression("Flow_0k7613y")).isEqualTo("=not(" + ProcessConstants.VAR_CAN_ISSUE + ")");
    }

    private String zeebeTaskType(String activityId) {
        return firstChild(elementById(activityId), ZEEBE, "taskDefinition").getAttribute("type");
    }

    private String candidateGroups(String taskId) {
        return firstChild(elementById(taskId), ZEEBE, "assignmentDefinition").getAttribute("candidateGroups");
    }

    private String sequenceTarget(String flowId) {
        return elementById(flowId).getAttribute("targetRef");
    }

    private Map<String, String> sequenceSourceAndTarget(String flowId) {
        Element flow = elementById(flowId);
        return Map.of("sourceRef", flow.getAttribute("sourceRef"), "targetRef", flow.getAttribute("targetRef"));
    }

    private String conditionExpression(String flowId) {
        return firstChild(elementById(flowId), BPMN, "conditionExpression").getTextContent().trim();
    }

    private Element elementById(String id) {
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element && id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        throw new AssertionError("BPMN element not found: " + id);
    }

    private Element firstChild(Element parent, String namespace, String localName) {
        NodeList children = parent.getElementsByTagNameNS(namespace, localName);
        assertThat(children.getLength())
                .as("Expected %s child on %s", localName, parent.getAttribute("id"))
                .isGreaterThan(0);
        return (Element) children.item(0);
    }

    private static Document readBpmn(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(path.toFile());
        } catch (Exception ex) {
            throw new AssertionError("Could not read BPMN file: " + path, ex);
        }
    }
}
