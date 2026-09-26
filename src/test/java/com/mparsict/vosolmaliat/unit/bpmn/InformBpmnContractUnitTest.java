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

class InformBpmnContractUnitTest {

    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String ZEEBE = "http://camunda.org/schema/zeebe/1.0";
    private static final String INFORM_PROCESS_ID = "Process_0lt64q4";
    private static final Path INFORM_BPMN = Path.of("src/main/resources/bpmn/Inform.bpmn");

    private final Document document = readBpmn(INFORM_BPMN);

    @Test
    void informProcessUsesExpectedCamunda8WorkerTypes() {
        Element process = elementById(INFORM_PROCESS_ID);

        assertThat(process.getAttribute("isExecutable")).isEqualTo("true");
        assertThat(zeebeTaskType("Activity_1lik1o7")).isEqualTo(ProcessConstants.JOB_CHECK_INFORM);
        assertThat(zeebeTaskType("Activity_1wj000f")).isEqualTo(ProcessConstants.JOB_SEND_TO_ELECTRONIC_NOTIFICATION);
        assertThat(zeebeTaskType("Activity_18aw0is")).isEqualTo(ProcessConstants.JOB_UPDATE_INFORM_STATUS);
        assertThat(zeebeTaskType("Activity_1ph17fo")).isEqualTo(ProcessConstants.JOB_UPDATE_INFORM_STATUS);
        assertThat(zeebeTaskType("Activity_0coidwa")).isEqualTo(ProcessConstants.JOB_SET_RE_INFORM_DATE);
        assertThat(zeebeTaskType("Activity_131cbua")).isEqualTo(ProcessConstants.JOB_SET_INQUIRE);
        assertThat(zeebeTaskType("Activity_077bco2")).isEqualTo(ProcessConstants.JOB_SEND_INFORM_DATA_TO_SANIM);
    }

    @Test
    void informMessageWaitsForElectronicNotificationCallbackByExecutionPaperId() {
        Element message = elementById("Message_1hqvd17");
        Element subscription = firstChild(message, ZEEBE, "subscription");

        assertThat(message.getAttribute("name")).isEqualTo(ProcessConstants.MESSAGE_EXECUTION_PAPER_INFORMED);
        assertThat(subscription.getAttribute("correlationKey"))
                .isEqualTo("=" + ProcessConstants.VAR_EXECUTION_PAPER_ID);
        assertThat(firstChild(elementById("Event_05e6j5b"), BPMN, "messageEventDefinition").getAttribute("messageRef"))
                .isEqualTo("Message_1hqvd17");
    }

    @Test
    void informGatewayBranchesByCanInformAndRetriesAfterReInformDate() {
        assertThat(conditionExpression("Flow_11rvp53")).isEqualTo("=" + ProcessConstants.VAR_CAN_INFORM);
        assertThat(conditionExpression("Flow_0nhulxv")).isEqualTo("=not(" + ProcessConstants.VAR_CAN_INFORM + ")");

        assertThat(sequenceSourceAndTarget("Flow_0nhulxv"))
                .containsEntry("targetRef", "Activity_1ph17fo");
        assertThat(sequenceSourceAndTarget("Flow_0iq4g6o"))
                .containsEntry("sourceRef", "Activity_1ph17fo")
                .containsEntry("targetRef", "Activity_0coidwa");
        assertThat(timerExpression("Event_0ijgi1v"))
                .isEqualTo("=" + ProcessConstants.VAR_RE_INFORM_DATE);
        assertThat(sequenceSourceAndTarget("Flow_0aunxa6"))
                .containsEntry("sourceRef", "Event_0ijgi1v")
                .containsEntry("targetRef", "Activity_1lik1o7");
    }

    @Test
    void successfulInformPathUpdatesLocalStatusAndSendsInformDateToSanimInParallel() {
        assertThat(sequenceSourceAndTarget("Flow_1yd4oyq"))
                .containsEntry("sourceRef", "Activity_1wj000f")
                .containsEntry("targetRef", "Event_05e6j5b");
        assertThat(sequenceSourceAndTarget("Flow_0a7ap8m"))
                .containsEntry("sourceRef", "Event_05e6j5b")
                .containsEntry("targetRef", "Gateway_1kqyfwh");
        assertThat(sequenceSourceAndTarget("Flow_0gp2thc"))
                .containsEntry("sourceRef", "Gateway_1kqyfwh")
                .containsEntry("targetRef", "Activity_18aw0is");
        assertThat(sequenceSourceAndTarget("Flow_1awjdrz"))
                .containsEntry("sourceRef", "Gateway_1kqyfwh")
                .containsEntry("targetRef", "Activity_077bco2");
        assertThat(sequenceSourceAndTarget("Flow_1ecgufz"))
                .containsEntry("sourceRef", "Activity_18aw0is")
                .containsEntry("targetRef", "Activity_131cbua");
    }

    private String zeebeTaskType(String activityId) {
        return firstChild(elementById(activityId), ZEEBE, "taskDefinition").getAttribute("type");
    }

    private String conditionExpression(String flowId) {
        return firstChild(elementById(flowId), BPMN, "conditionExpression").getTextContent().trim();
    }

    private String timerExpression(String eventId) {
        return firstChild(elementById(eventId), BPMN, "timeDate").getTextContent().trim();
    }

    private Map<String, String> sequenceSourceAndTarget(String flowId) {
        Element flow = elementById(flowId);
        return Map.of("sourceRef", flow.getAttribute("sourceRef"), "targetRef", flow.getAttribute("targetRef"));
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
