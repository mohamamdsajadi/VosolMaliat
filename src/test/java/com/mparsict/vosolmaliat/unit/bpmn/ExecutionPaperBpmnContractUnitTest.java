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
        assertThat(sequenceTarget("Flow_16w484y")).isEqualTo("Activity_0generalHeadReview");
    }

    @Test
    void issuanceTaskMustPublishPersistedPaperContextBeforeParallelBranchesStart() {
        assertThat(outputMappings("Activity_0rwz3ak"))
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, "=" + ProcessConstants.VAR_EXECUTION_PAPER_ID)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_CODE, "=" + ProcessConstants.VAR_EXECUTION_PAPER_CODE)
                .containsEntry(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, "=" + ProcessConstants.VAR_DEBT_PAPER_PAYLOAD)
                .containsEntry(ProcessConstants.VAR_TAX_UNIT_ID, "=" + ProcessConstants.VAR_TAX_UNIT_ID);
    }

    @Test
    void sanimIdTaskRequiresThePersistedExecutionPaperId() {
        assertThat(inputMappings("Activity_1f7l5yj"))
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID,
                        "=" + ProcessConstants.VAR_EXECUTION_PAPER_ID);
    }

    @Test
    void legalExecutionPaperStartsTabsareProcess() {
        assertThat(sequenceTarget("Flow_07v2mwm")).isEqualTo("Gateway_1g0dphg");
        assertThat(sequenceTarget("Flow_0dk43em")).isEqualTo("Activity_0aeytw4");
        Element calledElement = firstChild(elementById("Activity_0aeytw4"), ZEEBE, "calledElement");
        assertThat(calledElement.getAttribute("processId"))
                .isEqualTo(ProcessConstants.TABSARE_3_186_PROCESS_ID);
    }

    @Test
    void operationalUserTasksFollowTheBusinessRoleDelegationHierarchy() {
        Map<String, String> expectedCandidateGroups = Map.of(
                "Activity_0generalHeadReview", "GENERAL_OFFICE_HEAD",
                "Activity_0taxOfficeHeadReview", "TAX_OFFICE_HEAD",
                "Activity_0taxUnitHeadReview", "TAX_UNIT_HEAD",
                "Activity_0taxExpertProcess", "TAX_EXPERT"
        );

        expectedCandidateGroups.forEach((taskId, variable) ->
                assertThat(candidateGroups(taskId)).isEqualTo(variable));

        assertThat(sequenceSourceAndTarget("Flow_0generalReviewDecision"))
                .containsEntry("sourceRef", "Activity_0generalHeadReview")
                .containsEntry("targetRef", "Gateway_0generalReviewDecision");
        assertThat(sequenceSourceAndTarget("Flow_0generalHeadDone"))
                .containsEntry("sourceRef", "Gateway_0generalReviewDecision")
                .containsEntry("targetRef", "Activity_0taxOfficeHeadReview");
        assertThat(sequenceSourceAndTarget("Flow_0taxOfficeReviewDecision"))
                .containsEntry("sourceRef", "Activity_0taxOfficeHeadReview")
                .containsEntry("targetRef", "Gateway_0taxOfficeReviewDecision");
        assertThat(sequenceSourceAndTarget("Flow_0taxOfficeHeadDone"))
                .containsEntry("sourceRef", "Gateway_0taxOfficeReviewDecision")
                .containsEntry("targetRef", "Activity_0taxUnitHeadReview");
        assertThat(sequenceSourceAndTarget("Flow_0taxUnitReviewDecision"))
                .containsEntry("sourceRef", "Activity_0taxUnitHeadReview")
                .containsEntry("targetRef", "Gateway_0taxUnitReviewDecision");
        assertThat(sequenceSourceAndTarget("Flow_0taxUnitHeadDone"))
                .containsEntry("sourceRef", "Gateway_0taxUnitReviewDecision")
                .containsEntry("targetRef", "Activity_0taxExpertProcess");
    }

    @Test
    void everyOperationalReviewStepCanFinishThroughOneMergedReviewBranch() {
        Map.of(
                "Flow_0generalHeadFinish", "Gateway_0generalReviewDecision",
                "Flow_0taxOfficeHeadFinish", "Gateway_0taxOfficeReviewDecision",
                "Flow_0taxUnitHeadFinish", "Gateway_0taxUnitReviewDecision"
        ).forEach((flowId, sourceId) -> {
            assertThat(sequenceSourceAndTarget(flowId))
                    .containsEntry("sourceRef", sourceId)
                    .containsEntry("targetRef", "Gateway_0reviewMerge");
            assertThat(conditionExpression(flowId))
                    .isEqualTo("=" + ProcessConstants.VAR_FINISH_EXECUTION_PAPER_CHECK + " = true");
        });

        assertThat(sequenceSourceAndTarget("Flow_0jnz16g"))
                .containsEntry("sourceRef", "Activity_0taxExpertProcess")
                .containsEntry("targetRef", "Gateway_0reviewMerge");
        assertThat(sequenceSourceAndTarget("Flow_0reviewBranchDone"))
                .containsEntry("sourceRef", "Gateway_0reviewMerge")
                .containsEntry("targetRef", "Gateway_0e7ujls");
        assertThat(incomingFlowIds("Gateway_0e7ujls"))
                .containsExactlyInAnyOrder(
                        "Flow_0reviewBranchDone",
                        "Flow_0aj7mt3",
                        "Flow_19yszz7",
                        "Flow_1i7jq54"
                );
    }

    @Test
    void issuanceGatewayConditionsUseTheWorkerOutputVariable() {
        assertThat(outputMappings("Activity_1ahtl92"))
                .containsEntry(ProcessConstants.VAR_CAN_ISSUE, "=" + ProcessConstants.VAR_CAN_ISSUE);

        assertThat(sequenceSourceAndTarget("Flow_0eioah5"))
                .containsEntry("sourceRef", "Gateway_1uchrac")
                .containsEntry("targetRef", "Activity_0rwz3ak");
        assertThat(conditionExpression("Flow_0eioah5")).isEqualTo("=" + ProcessConstants.VAR_CAN_ISSUE);

        assertThat(sequenceSourceAndTarget("Flow_0k7613y"))
                .containsEntry("sourceRef", "Gateway_1uchrac")
                .containsEntry("targetRef", "Activity_010iyzn");
        assertThat(conditionExpression("Flow_0k7613y")).isEqualTo("=not(" + ProcessConstants.VAR_CAN_ISSUE + ")");
    }

    private String zeebeTaskType(String activityId) {
        return firstChild(elementById(activityId), ZEEBE, "taskDefinition").getAttribute("type");
    }

    private String candidateGroups(String taskId) {
        return firstChild(elementById(taskId), ZEEBE, "assignmentDefinition").getAttribute("candidateGroups");
    }

    private Map<String, String> outputMappings(String activityId) {
        NodeList outputs = elementById(activityId).getElementsByTagNameNS(ZEEBE, "output");
        Map<String, String> mappings = new HashMap<>();
        for (int i = 0; i < outputs.getLength(); i++) {
            Element output = (Element) outputs.item(i);
            mappings.put(output.getAttribute("target"), output.getAttribute("source"));
        }
        return mappings;
    }

    private Map<String, String> inputMappings(String activityId) {
        NodeList inputs = elementById(activityId).getElementsByTagNameNS(ZEEBE, "input");
        Map<String, String> mappings = new HashMap<>();
        for (int i = 0; i < inputs.getLength(); i++) {
            Element input = (Element) inputs.item(i);
            mappings.put(input.getAttribute("target"), input.getAttribute("source"));
        }
        return mappings;
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

    private java.util.List<String> incomingFlowIds(String activityId) {
        NodeList incoming = elementById(activityId).getElementsByTagNameNS(BPMN, "incoming");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < incoming.getLength(); i++) {
            result.add(incoming.item(i).getTextContent().trim());
        }
        return result;
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
