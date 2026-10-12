package com.mparsict.vosolmaliat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.VosolMaliatApplication;
import com.mparsict.vosolmaliat.application.service.DebtPaperXmlReader;
import com.mparsict.vosolmaliat.domain.model.masterdata.*;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.*;
import io.camunda.client.CamundaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.w3c.dom.Element;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in real HTTP/security/Camunda/SQLite test. Never runs in the ordinary test suite. */
@EnabledIfSystemProperty(named = "ef03.live", matches = "true")
@SpringBootTest(classes = VosolMaliatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO", "logging.level.org.springframework=INFO",
                "logging.level.org.springframework.web=INFO", "logging.level.org.springframework.jdbc=INFO",
                "logging.level.org.springframework.boot=INFO", "logging.level.com.mparsict.vosolmaliat=INFO"})
class Ef03PipelineLiveTest {
    static final String RUN = "ef03_test_" + UUID.randomUUID().toString().replace("-", "");
    static final Path OUTPUT = Path.of(".local", "ef03-tests", RUN).toAbsolutePath();
    static final String USER = "ef03-test-sender";
    static final String PASSWORD = UUID.randomUUID().toString();
    static final String ACCESS = UUID.randomUUID().toString();
    static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";

    @DynamicPropertySource
    static void isolatedConfiguration(DynamicPropertyRegistry properties) throws Exception {
        Files.createDirectories(OUTPUT);
        Files.setPosixFilePermissions(OUTPUT, PosixFilePermissions.fromString("rwx------"));
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + OUTPUT.resolve("test.sqlite"));
        properties.add("app.integration.debt-paper.security.username", () -> USER);
        properties.add("app.integration.debt-paper.security.password-hash", () ->
                PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(PASSWORD));
        properties.add("app.integration.debt-paper.security.access-ids", () -> ACCESS);
        properties.add("app.keycloak.bootstrap-users.enabled", () -> false);
        properties.add("app.demo.enabled", () -> false);
        properties.add("app.ef03.import-samples", () -> false);
        properties.add("app.test.database-reset.enabled", () -> false);
        properties.add("server.address", () -> "127.0.0.1");
        properties.add("debug", () -> false);
        properties.add("logging.level.root", () -> "INFO");
        properties.add("logging.level.org.springframework", () -> "INFO");
        properties.add("logging.level.org.springframework.web", () -> "INFO");
        properties.add("logging.level.org.springframework.jdbc", () -> "INFO");
        properties.add("logging.level.org.springframework.boot", () -> "INFO");
        // Existing applications on the broker cannot consume this run's unique job types.
        properties.add("camunda.client.worker.defaults.enabled", () -> false);
        properties.add("app.issuance.process-id", () -> RUN);
        properties.add("logging.level.com.mparsict.vosolmaliat", () -> "INFO");
        System.out.println("EF03_RUN_DIRECTORY=" + OUTPUT);
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired CamundaClient camunda;
    @Autowired CheckIssuancePossibilityWorker checkWorker;
    @Autowired IssueExecutionPaperWorker issueWorker;
    @Autowired SendRejectionResponseWorker rejectionWorker;
    @Autowired DebtPaperXmlReader xmlReader;
    @Autowired GeneralDirectorateOfTaxAffairsRepository directorates;
    @Autowired TaxAffairsOfficeRepository offices;
    @Autowired ActivityTypeRepository activities;
    @Autowired ExecutionPaperRejectionRuleRepository rules;
    @Autowired JdbcTemplate db;

    record Source(int row, Map<String, String> columns, Map<String, String> tags) {}

    List<Source> readSource() throws Exception {
        Path source = Path.of(System.getProperty("ef03.source", "docs/paper-types-examples/ef03.ods"));
        try (var zip = new ZipFile(source.toFile(), StandardCharsets.UTF_8)) {
            var document = xmlReader.parse(new String(zip.getInputStream(zip.getEntry("content.xml")).readAllBytes(), StandardCharsets.UTF_8));
            var rows = document.getElementsByTagNameNS(TABLE_NS, "table-row");
            List<String> headers = null;
            List<Source> result = new ArrayList<>();
            for (int n = 0; n < rows.getLength(); n++) {
                var cells = ((Element) rows.item(n)).getElementsByTagNameNS(TABLE_NS, "table-cell");
                List<String> values = new ArrayList<>();
                for (int c = 0; c < cells.getLength(); c++) {
                    var cell = (Element) cells.item(c);
                    String repeats = cell.getAttributeNS(TABLE_NS, "number-columns-repeated");
                    int count = repeats.isBlank() ? 1 : Integer.parseInt(repeats);
                    // Preserve XML cell text verbatim, including surrounding whitespace.
                    for (int r = 0; r < Math.min(count, 100); r++) values.add(cell.getTextContent());
                }
                if (values.stream().allMatch(String::isBlank)) continue;
                if (headers == null) { headers = values; continue; }
                Map<String, String> columns = new LinkedHashMap<>();
                for (int c = 0; c < Math.min(headers.size(), values.size()); c++) columns.put(headers.get(c).trim(), values.get(c));
                if (!"LTR_EF03".equals(columns.getOrDefault("TypeID", "").trim())) continue;
                result.add(new Source(n + 1, columns, xmlReader.read(columns.get("XML"))));
            }
            return result;
        }
    }

    void seedReferenceData(List<Source> sources) {
        Map<String, GeneralDirectorateOfTaxAffairs> generals = new HashMap<>();
        Set<String> officeCodes = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (var source : sources) {
            var tags = source.tags();
            String generalName = tags.get("h");
            var general = generals.computeIfAbsent(generalName, name -> directorates.save(
                    GeneralDirectorateOfTaxAffairs.builder().generalDirectorateName(name)
                            .postalCode("").address("").phone("").build()));
            String officeCode = officeCode(tags.get("i"));
            if (officeCodes.add(officeCode)) offices.save(TaxAffairsOffice.builder()
                    .officeName(tags.get("i")).generalDirectorateOfTaxAffairs(general)
                    .address(tags.getOrDefault("l", "")).phone(tags.getOrDefault("k", ""))
                    .postalCode(tags.getOrDefault("j", "")).build());
            String title = tags.get("u");
            if (title != null && !title.isBlank() && titles.add(title)) activities.save(ActivityType.builder()
                    .intaCode("TEST-" + titles.size()).title(title).build());
        }
    }

    static String officeCode(String value) {
        var matcher = java.util.regex.Pattern.compile("[0-9]+").matcher(value);
        Set<String> codes = new HashSet<>();
        while (matcher.find()) codes.add(matcher.group());
        if (codes.size() != 1) throw new IllegalArgumentException("Ambiguous source office code");
        return codes.iterator().next();
    }

    Map<String, Object> envelope(Source source) throws Exception {
        var c = source.columns();
        String xml = c.get("XML");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nationalID", c.get("NationalID").trim());
        result.put("xml", xml);
        result.put("hash", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(xml.getBytes(StandardCharsets.UTF_8))));
        result.put("produceDate", OffsetDateTime.now(ZoneOffset.UTC).toString());
        result.put("referenceID", c.get("RefrenceID").trim());
        result.put("followCode", c.get("FollowCode").trim());
        result.put("secondParameter", c.get("ID").trim());
        result.put("year", Integer.parseInt(source.tags().get("c")));
        result.put("period", 1);
        result.put("taxOfficeID", officeCode(source.tags().get("i")));
        result.put("taxAmount", source.tags().getOrDefault("cb", "0"));
        result.put("taxSource", "TAXB");
        result.put("announcementType", "LTR_EF03");
        return result;
    }

    HttpResponse<String> send(Map<String, Object> payload, boolean authenticated, String access) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/debt-papers/push"))
                .timeout(Duration.ofSeconds(135)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        if (authenticated) builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)));
        if (access != null) builder.header("AccessID", access);
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    void report(Map<String, Object> value) throws Exception {
        Files.writeString(OUTPUT.resolve("results.jsonl"), json.writeValueAsString(value) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Test
    void sendSourceDocumentsThroughAuthenticatedHttpAndRealCamunda() throws Exception {
        var sources = readSource();
        int limit = Math.min(Integer.getInteger("ef03.limit", 1), sources.size());
        assertThat(limit).isPositive();
        sources = sources.subList(0, limit);
        seedReferenceData(sources);
        Files.writeString(OUTPUT.resolve("run.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                "processId", RUN, "username", USER, "authority", "DEBT_PAPER_PUSH", "rows", limit,
                "database", OUTPUT.resolve("test.sqlite").toString(), "httpPort", port,
                "source", System.getProperty("ef03.source", "docs/paper-types-examples/ef03.ods"))));
        var credentials = OUTPUT.resolve("credentials.json");
        Files.createFile(credentials, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(credentials, json.writeValueAsString(Map.of("username", USER, "password", PASSWORD, "accessID", ACCESS)));

        var first = envelope(sources.getFirst());
        assertThat(send(first, false, null).statusCode()).isEqualTo(401);
        assertThat(send(first, true, "wrong-access-id").statusCode()).isEqualTo(403);
        assertThat(db.queryForObject("select count(*) from debt_paper_push_record", Long.class)).isZero();
        report(Map.of("stage", "authentication", "missingCredentials", 401, "invalidAccessID", 403, "storedRows", 0));

        String bpmn = Files.readString(Path.of("src/main/resources/bpmn/ExecutionPapers.bpmn"))
                .replace(ProcessConstants.PROCESS_ID, RUN)
                .replace(ProcessConstants.JOB_CHECK_ISSUANCE, RUN + "_check")
                .replace(ProcessConstants.JOB_ISSUE_PAPER, RUN + "_issue")
                .replace(ProcessConstants.JOB_SEND_REJECTION, RUN + "_reject")
                .replace("retries=\"\"", "retries=\"3\"");
        camunda.newDeployResourceCommand().addResourceStringUtf8(bpmn, RUN + ".bpmn").send().join();
        try (var check = camunda.newWorker().jobType(RUN + "_check").handler(checkWorker::handle).pollInterval(Duration.ofMillis(100)).open();
             var issue = camunda.newWorker().jobType(RUN + "_issue").handler(issueWorker::handle).pollInterval(Duration.ofMillis(100)).open();
             var rejection = camunda.newWorker().jobType(RUN + "_reject").handler(rejectionWorker::handle).pollInterval(Duration.ofMillis(100)).open()) {
            int issued = 0, rejected = 0;
            boolean duplicateChecked = false;
            for (var source : sources) {
                var payload = envelope(source);
                report(Map.of("stage", "sending", "row", source.row(), "referenceID", payload.get("referenceID"),
                        "startedAt", Instant.now().toString()));
                long started = System.nanoTime();
                var response = send(payload, true, ACCESS);
                JsonNode body = json.readTree(response.body());
                String reference = payload.get("referenceID").toString();
                var receipt = db.queryForMap("select * from debt_paper_push_record where reference_id=?", reference);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("row", source.row()); item.put("referenceID", reference);
                item.put("httpStatus", response.statusCode()); item.put("response", body);
                item.put("receiptId", receipt.get("id")); item.put("status", receipt.get("processing_status"));
                item.put("issuanceRequestId", receipt.get("issuance_request_id"));
                item.put("processInstanceKey", receipt.get("process_instance_key"));
                item.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
                report(item);
                assertThat(receipt.get("xml_payload")).isEqualTo(source.columns().get("XML"));
                if (response.statusCode() == 200) {
                    long paper = body.path("apiGatewayData").path("executionPaperID").asLong();
                    assertThat(paper).isPositive();
                    assertThat(((Number) receipt.get("execution_paper_id")).longValue()).isEqualTo(paper);
                    assertThat(receipt.get("process_instance_key")).isNotNull();
                    verifyStoredPaper(paper, source, ((Number) receipt.get("issuance_request_id")).longValue());
                    issued++;
                    if (!duplicateChecked) {
                        var duplicate = send(payload, true, ACCESS);
                        assertThat(duplicate.statusCode()).isEqualTo(409);
                        assertThat(json.readTree(duplicate.body()).path("apiGatewayData").path("executionPaperID").asLong()).isEqualTo(paper);
                        assertThat(db.queryForObject("select count(*) from execution_paper", Long.class)).isEqualTo((long) issued);
                        report(Map.of("stage", "duplicate", "referenceID", reference, "httpStatus", 409, "paperID", paper));
                        duplicateChecked = true;
                    }
                } else {
                    // Preserve and report source-data validation failures; never repair XML.
                    assertThat(response.statusCode()).as(response.body()).isEqualTo(422);
                    assertThat(receipt.get("execution_paper_id")).isNull();
                    rejected++;
                }
                System.out.printf("EF03 progress %d/%d: HTTP %d, row %d%n", issued + rejected, limit, response.statusCode(), source.row());
            }
            assertThat(issued).isPositive();
            rules.save(com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule.builder()
                    .errorCode("TEST_RULE_BLOCK").errorMessage("Controlled test rejection")
                    .startDate("2000-01-01").endDate("2099-12-31").documentNo(RUN).filePath("test-fixture")
                    .ruleJson("""
                            {"logicalOperator":"AND","rejectionReasonCode":"TEST_RULE_BLOCK",
                             "rejectionReasonDescription":"Controlled test rejection",
                             "conditions":[{"field":"taxSource","operator":"EQUALS","value":"TAXB"}]}
                            """).build());
            var blockedPayload = envelope(sources.getFirst());
            blockedPayload.put("referenceID", RUN + "_rule_rejection");
            var blocked = send(blockedPayload, true, ACCESS);
            assertThat(blocked.statusCode()).isEqualTo(422);
            assertThat(json.readTree(blocked.body()).path("apiGatewayData").path("rejectionReasonCode").asText()).isEqualTo("TEST_RULE_BLOCK");
            assertThat(db.queryForObject("select count(*) from execution_paper", Long.class)).isEqualTo((long) issued);
            var blockedReceipt = db.queryForMap("select * from debt_paper_push_record where reference_id=?", blockedPayload.get("referenceID"));
            assertThat(blockedReceipt.get("process_instance_key")).isNotNull();
            assertThat(blockedReceipt.get("processing_status")).isEqualTo("REJECTED_BY_RULE");
            report(Map.of("stage", "rule-rejection", "httpStatus", 422,
                    "processInstanceKey", blockedReceipt.get("process_instance_key"), "response", json.readTree(blocked.body())));
            assertThat(db.queryForList("PRAGMA foreign_key_check")).isEmpty();
            Files.writeString(OUTPUT.resolve("summary.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "issued", issued, "rejected", rejected, "total", limit, "xmlUnchanged", true,
                    "authenticationChecked", true, "duplicateChecked", duplicateChecked,
                    "ruleRejectionChecked", true, "processId", RUN)));
        }
    }

    void verifyStoredPaper(long paper, Source source, long requestId) throws Exception {
        var debt = db.queryForMap("select * from debt_information where execution_paper_id=?", paper);
        for (var field : Map.of("cb", "tax_component", "gb", "total_penalties", "eb", "total_payments", "hb", "outstanding_tax_debt", "db", "total_remissions").entrySet()) {
            var expected = new java.math.BigDecimal(source.tags().getOrDefault(field.getKey(), "0"));
            assertThat(new java.math.BigDecimal(debt.get(field.getValue()).toString())).isEqualByComparingTo(expected);
        }
        for (String table : List.of("execution_paper_state_history", "debt_information_history", "execution_workflow_state")) {
            assertThat(db.queryForObject("select count(*) from " + table + " where execution_paper_id=?", Long.class, paper)).isPositive();
        }
        assertThat(db.queryForObject("select count(*) from execution_paper e join final_tax_debt_paper f on f.id=e.final_tax_debt_paper_id join taxpayer t on t.id=f.taxpayer_id where e.id=?", Long.class, paper)).isEqualTo(1L);
        assertThat(db.queryForObject("select xml from execution_paper_issuance_request where id=?", String.class, requestId)).isEqualTo(source.columns().get("XML"));
    }
}
