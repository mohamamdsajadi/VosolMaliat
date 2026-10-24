package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.LegalRestrictionResponses.*;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

/** Read-only tracking. No restriction is sent, confirmed or released through this API. */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class LegalRestrictionQueryService {
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final Map<String, String> EVENTS = Map.of(
        "OFFICIAL_GAZETTE_REQUEST_SENT", "ارسال درخواست شناسایی اعضای هیئت‌مدیره",
        "OFFICIAL_GAZETTE_RESPONSE_SENT", "دریافت پاسخ شناسایی اشخاص",
        "RELATED_PERSONS_FILTERED_BY_DEBT_PAPER_DATE", "تعیین اشخاص مرتبط در تاریخ ایجاد بدهی",
        "RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE", "ذخیره اطلاعات اشخاص مرتبط",
        "RELATED_PERSONS_LINKED_TO_EXECUTION_PAPER", "ایجاد ارتباط اشخاص و برگ اجرا",
        "RESTRICTION_REQUEST_SENT", "ارسال درخواست اعمال محدودیت",
        "RESTRICTION_REQUEST_CONFIRMED", "دریافت تأیید اعمال محدودیت");
    private final FakeRestrictionRequestRepository requests;
    private final FakeOfficialGazetteRequestRepository gazettes;
    private final CompanyRestrictionRepository restrictions;
    private final ExecutionPaperRepository papers;
    private final ExecutionPaperService paperService;
    private final ExecutionPaperVisibility visibility;
    private final SecurityIdentityService identity;
    private final AuditLogRepository audit;
    private final AppUserProfileRepository profiles;
    private final ObjectMapper json;

    public Page list(int page, int size, String search, String status, LocalDate from, LocalDate to) {
        if (page < 0 || size < 1 || size > 100 || from != null && to != null && from.isAfter(to)) throw bad("محدوده جستجو معتبر نیست.");
        if (status != null && !status.isBlank() && !Set.of("SENT", "CONFIRMED", "FAILED").contains(status)) throw bad("وضعیت درخواست معتبر نیست.");
        var rows = requests.findAll(scope(viewer())).stream().map(this::row).toList();
        String term = normalize(search).toLowerCase(Locale.ROOT);
        // Legacy source timestamps are strings in mixed formats. Normalize before filtering/paging.
        var selected = rows.stream().filter(row -> status == null || status.isBlank() || row.status().equals(status))
                .filter(row -> term.isBlank() || java.util.stream.Stream.of(row.executionPaperCode(), row.taxpayerName(), row.taxpayerNationalId(), row.trackingCode())
                        .filter(Objects::nonNull).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(term)))
                .filter(row -> from == null || row.sentAt() != null && !row.sentAt().atZone(TEHRAN).toLocalDate().isBefore(from))
                .filter(row -> to == null || row.sentAt() != null && !row.sentAt().atZone(TEHRAN).toLocalDate().isAfter(to))
                .sorted(Comparator.comparing(Row::sentAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Row::id, Comparator.reverseOrder())).toList();
        int start = (int)Math.min(selected.size(), (long)page * size);
        return new Page(selected.subList(start, Math.min(start + size, selected.size())), selected.size(), (selected.size() + size - 1) / size, page, size,
                new Counts(rows.size(), rows.stream().filter(row -> "SENT".equals(row.status())).count(),
                        rows.stream().filter(row -> "CONFIRMED".equals(row.status())).count(), rows.stream().filter(row -> "FAILED".equals(row.status())).count()));
    }

    public Detail detail(Long id) {
        var user = viewer();
        var request = requests.findOne(scope(user).and((root, query, cb) -> cb.equal(root.get("id"), id)))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "درخواست در محدوده دسترسی شما یافت نشد."));
        var gazette = gazettes.findByRequestId(request.getOfficialGazetteRequestId()).orElse(null);
        Instant begin = gazette == null ? instant(request.getSentAt()) : instant(gazette.getSentAt());
        Instant end = instant(request.getConfirmedAt());
        // Keep the history tied to this request, including when a paper has several requests.
        var next = requests.findAllByExecutionPaperIdOrderByIdDesc(request.getExecutionPaperId()).stream()
                .filter(other -> !other.getId().equals(id))
                .map(other -> gazettes.findByRequestId(other.getOfficialGazetteRequestId())
                        .map(source -> instant(source.getSentAt())).orElse(instant(other.getSentAt())))
                .filter(Objects::nonNull).filter(time -> begin != null && time.isAfter(begin)).min(Comparator.naturalOrder()).orElse(null);
        Instant historyEnd = next == null ? end : end != null && end.isBefore(next) ? end : next.minusNanos(1);
        var names = new HashMap<String, String>();
        var history = audit.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("EXECUTION_PAPER", request.getExecutionPaperId().toString()).stream()
                .filter(event -> EVENTS.containsKey(event.getAction()))
                .filter(event -> begin == null || !auditInstant(event.getCreatedAt(), request).isBefore(begin))
                .filter(event -> historyEnd == null || !auditInstant(event.getCreatedAt(), request).isAfter(historyEnd))
                .map(event -> new Event(event.getId(), EVENTS.get(event.getAction()), names.computeIfAbsent(
                        Objects.toString(event.getActorUsername(), "SYSTEM"), this::actorName), auditInstant(event.getCreatedAt(), request), note(event.getNewValue()))).toList();
        var paperRestrictions = restrictions.findByExecutionPaperIdOrderByIdAsc(request.getExecutionPaperId()).stream().map(restriction -> {
            var person = restriction.getNaturalPerson();
            return new Restriction(name(person.getFirstName(), person.getLastName()),
                    first(person.getNationalCode(), person.getFida()), restriction.getRestrictionState().name(),
                    restriction.getRestrictionState().label(), restriction.getRestrictionTrackingCode(),
                    auditInstant(restriction.getRestrictionDate(), request), auditInstant(restriction.getReleaseDate(), request), restriction.getReleaseTrackingCode());
        }).toList();
        return new Detail(row(request), gazette == null ? null : gazette.getDebtPaperDate(), request.getErrorMessage(), subjects(request.getPayload()), paperRestrictions, history);
    }

    private Specification<FakeRestrictionRequest> scope(CurrentUserContext user) {
        var assigned = visibility.assignedOnly(user) ? visibility.paperIds(user) : null;
        return (root, query, cb) -> {
            var allowedPapers = query.subquery(Long.class);
            var paper = allowedPapers.from(ExecutionPaper.class);
            var source = paper.join("finalTaxDebtPaper");
            var taxpayer = source.join("taxpayer");
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isNotNull(taxpayer.get("legalPerson")));
            if (!user.isAdmin() && (user.businessRole() == null || !user.businessRole().isNationalScope())) {
                Long directorate = user.organization() == null ? null : user.organization().generalDirectorateId();
                predicates.add(cb.equal(source.get("taxAffairsOffice").get("generalDirectorateOfTaxAffairs").get("id"), directorate == null ? -1L : directorate));
            }
            if (assigned != null) predicates.add(paper.get("id").in(assigned.isEmpty() ? List.of(-1L) : assigned));
            allowedPapers.select(paper.get("id")).where(predicates.toArray(Predicate[]::new));
            return root.get("executionPaperId").in(allowedPapers);
        };
    }

    private Row row(FakeRestrictionRequest request) {
        var summary = paperService.getSummary(request.getExecutionPaperId());
        var paper = papers.findById(request.getExecutionPaperId()).orElseThrow();
        return new Row(request.getId(), request.getOfficialGazetteRequestId(), summary.id(), summary.uniqueCode(),
                summary.taxpayerName(), summary.taxpayerNationalId(), paper.getFinalTaxDebtPaper().getTaxAffairsOffice()
                .getGeneralDirectorateOfTaxAffairs().getGeneralDirectorateName(),
                "PENDING".equals(request.getStatus()) ? "SENT" : request.getStatus(), Objects.requireNonNullElse(request.getRelatedPersonCount(), 0),
                instant(request.getSentAt()), instant(request.getConfirmedAt()), true);
    }
    private List<Subject> subjects(String payload) {
        if (payload == null || payload.isBlank()) return List.of();
        try {
            var node = json.readTree(payload);
            if (!node.isArray()) throw new IllegalArgumentException();
            var result = new ArrayList<Subject>();
            for (var person : node) {
                boolean legal = "LEGAL".equals(value(person, "personType"));
                var representative = person.path("legalRepresentative");
                result.add(new Subject(legal ? value(person, "companyName") : name(value(person, "firstName"), value(person, "lastName")),
                        legal ? value(person, "nationalId") : first(value(person, "nationalCode"), value(person, "fida")), legal ? "LEGAL" : "NATURAL",
                        postTitle(value(person, "postCode")), value(person, "startDate"), value(person, "endDate"),
                        legal ? name(value(representative, "firstName"), value(representative, "lastName")) : null));
            }
            return result;
        } catch (Exception ex) { throw new ResponseStatusException(CONFLICT, "اطلاعات اشخاص این درخواست قابل خواندن نیست."); }
    }
    private String actorName(String username) {
        if ("SYSTEM".equalsIgnoreCase(username)) return "سامانه";
        return profiles.findByUsername(username).map(profile -> first(name(profile.getFirstName(), profile.getLastName()), username)).orElse(username);
    }
    private String note(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            var node = json.readTree(payload);
            if (node.has("selectedCount")) return "انتخاب " + node.path("selectedCount").asText() + " شخص از " + node.path("receivedCount").asText() + " شخص دریافت‌شده";
            if (node.has("relatedPersons")) return "تعداد اشخاص: " + node.path("relatedPersons").size();
            if (node.has("relatedPersonCount")) return "تعداد اشخاص ارسالی: " + node.path("relatedPersonCount").asText();
            return null;
        } catch (Exception ignored) { return payload.length() <= 250 ? payload : null; }
    }
    private CurrentUserContext viewer() { var user = identity.currentUser(); if (!user.hasPermission("LEGAL_RESTRICTION_VIEW")) throw new AccessDeniedException("دسترسی مشاهده محدودیت‌های قانونی فعال نیست."); return user; }
    private String normalize(String value) { return com.mparsict.vosolmaliat.domain.value.PersianDates.digits(value == null ? "" : value).trim(); }
    private String postTitle(String value) { if (value == null) return null; try { return RelatedPersonPostCode.valueOf(value).label(); } catch (IllegalArgumentException ex) { try { return RelatedPersonPostCode.fromCompanyRegistrationCode(value).label(); } catch (IllegalArgumentException ignored) { return value; } } }
    private String value(JsonNode node, String key) { return node.path(key).isNull() || node.path(key).isMissingNode() ? null : node.path(key).asText(); }
    private String name(String first, String last) { return (Objects.toString(first, "") + " " + Objects.toString(last, "")).trim(); }
    private String first(String first, String second) { return first == null || first.isBlank() ? second : first; }
    private Instant instant(LocalDateTime time) { return time == null ? null : time.atZone(TEHRAN).toInstant(); }
    private Instant auditInstant(LocalDateTime time, FakeRestrictionRequest request) {
        // SQLite's driver decodes SQL-imported epoch milliseconds in the JVM zone.
        // Normal integration rows contain local business time instead.
        return time == null ? null : request.getCreatedBy() != null && request.getCreatedBy().startsWith("MOCK_TEHRAN")
                ? time.atZone(ZoneId.systemDefault()).toInstant() : instant(time);
    }
    private Instant instant(String time) { if (time == null || time.isBlank()) return null; try { return Instant.parse(time); } catch (Exception ignored) { try { return OffsetDateTime.parse(time).toInstant(); } catch (Exception ignoredToo) { try { return instant(LocalDateTime.parse(time)); } catch (Exception invalid) { return null; } } } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(BAD_REQUEST, message); }
}
