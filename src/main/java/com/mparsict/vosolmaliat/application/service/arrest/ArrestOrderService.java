package com.mparsict.vosolmaliat.application.service.arrest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.ArrestOrderRequests.*;
import com.mparsict.vosolmaliat.application.dto.response.ArrestOrderResponses.*;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage.*;
import static com.mparsict.vosolmaliat.domain.enums.BusinessRole.*;
import static com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderWorkflowEngine.Action.*;
import static org.springframework.http.HttpStatus.*;

@Service @RequiredArgsConstructor
public class ArrestOrderService {
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final String SPECIAL_TITLE = "اداره وصول و اجرای ویژه";
    private final ArrestOrderRepository orders;
    private final ArrestOrderEventRepository events;
    private final ArrestOrderCandidateRepository candidates;
    private final ExecutionPaperRepository papers;
    private final ExecutionPaperNotificationRepository notifications;
    private final DebtInformationRepository debts;
    private final ExecutionWorkflowStateRepository workflowStates;
    private final AppUserProfileRepository profiles;
    private final SecurityIdentityService identity;
    private final ExecutionPaperService paperService;
    private final AuditLogService audit;
    private final UniqueCodeGenerator databaseWriter;
    private final ArrestOrderWorkflowEngine engine;
    private final ObjectMapper json;
    private final com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility visibility;

    @Transactional(readOnly = true)
    public Page<Candidate> candidates(int page, int size, String search) {
        var user = viewer();
        if (!canStart(user)) return new Page<>(List.of(), 0, 0, page, size);
        var result = candidates.findCandidates(national(user), directorate(user), LocalDateTime.now(TEHRAN).minusDays(30),
                clean(search), pagination(page, size));
        return new Page<>(result.stream().map(paper -> {
            var notification = notifications.findByExecutionPaperId(paper.getId()).orElseThrow();
            return new Candidate(paperService.getSummary(paper.getId()), paper.getFinalTaxDebtPaper().getTaxAffairsOffice()
                    .getGeneralDirectorateOfTaxAffairs().getGeneralDirectorateName(),
                    notification.paymentDeadline().atZone(TEHRAN).toInstant());
        }).toList(), result.getTotalElements(), result.getTotalPages(), page, size);
    }

    @Transactional(readOnly = true)
    public Page<Row> list(int page, int size, String search, String stage, boolean mine, Long executionPaperId) {
        return list(page, size, search, stage, mine, executionPaperId, false);
    }

    @Transactional(readOnly = true)
    public Page<Row> list(int page, int size, String search, String stage, boolean mine, Long executionPaperId, boolean history) {
        var user = viewer();
        var visibleIds = visibility.assignedOnly(user) ? visibility.paperIds(user) : null;
        var actedIds = history ? events.findActedOrderIds(user.keycloakUserId(), user.username()) : List.<Long>of();
        Specification<ArrestOrder> specification = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            var paper = root.join("executionPaper");
            if (visibleIds != null) predicates.add(paper.get("id").in(visibleIds.isEmpty() ? List.of(-1L) : visibleIds));
            if (executionPaperId != null) predicates.add(cb.equal(paper.get("id"), executionPaperId));
            if (!national(user)) predicates.add(cb.equal(paper.get("finalTaxDebtPaper").get("taxAffairsOffice")
                    .get("generalDirectorateOfTaxAffairs").get("id"), directorate(user) == null ? -1L : directorate(user)));
            if (!clean(search).isEmpty()) predicates.add(cb.like(paper.get("executionPaperUniqueCode"), "%" + clean(search) + "%"));
            if (!clean(stage).isEmpty()) {
                try { predicates.add(cb.equal(root.get("stage"), com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage.valueOf(stage))); }
                catch (IllegalArgumentException ex) { throw bad("وضعیت انتخاب‌شده معتبر نیست."); }
            }
            if (mine || history) {
                var assignee = root.join("assignee", jakarta.persistence.criteria.JoinType.LEFT);
                var own = cb.equal(assignee.get("keycloakUserId"), user.keycloakUserId());
                var queue = user.businessRole() == null ? cb.disjunction() : cb.and(cb.isNull(root.get("assignee")),
                        cb.equal(root.get("candidateRole"), user.businessRole()));
                var stages = new ArrayList<com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage>();
                if (user.hasPermission("ARREST_ORDER_START") && (user.isAdmin() || user.businessRole() == SPECIAL_EXECUTION_DEPARTMENT_HEAD)) stages.add(SPECIAL_DEPARTMENT_REVIEW);
                if (user.hasPermission("ARREST_ORDER_APPROVE") && (user.isAdmin() || user.businessRole() == DIRECTORATE_EXECUTION_UNIT_HEAD || user.businessRole() == SPECIAL_EXECUTION_DEPARTMENT_HEAD)) stages.add(AWAITING_APPROVAL);
                if (user.hasPermission("ARREST_ORDER_ASSIGN") && (user.isAdmin() || user.businessRole() == DIRECTORATE_EXECUTION_TEAM_SUPERVISOR)) stages.add(AWAITING_OFFICER_ASSIGNMENT);
                var pending = cb.and(cb.or(own, queue), root.get("stage").in(stages));
                if (history) {
                    var reviewer = root.join("reviewer", jakarta.persistence.criteria.JoinType.LEFT);
                    var supervisor = root.join("supervisor", jakarta.persistence.criteria.JoinType.LEFT);
                    var officer = root.join("officer", jakarta.persistence.criteria.JoinType.LEFT);
                    var related = cb.or(own, cb.equal(reviewer.get("keycloakUserId"), user.keycloakUserId()),
                            cb.and(cb.isNotNull(root.get("issuedAt")), cb.equal(supervisor.get("keycloakUserId"), user.keycloakUserId())),
                            cb.and(cb.equal(root.get("stage"), ASSIGNED_TO_OFFICER), cb.equal(officer.get("keycloakUserId"), user.keycloakUserId())),
                            root.get("id").in(actedIds.isEmpty() ? List.of(-1L) : actedIds));
                    predicates.add(cb.and(related, cb.not(pending)));
                } else predicates.add(pending);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = orders.findAll(specification, pagination(page, size));
        return new Page<>(result.stream().map(order -> row(order, user)).toList(), result.getTotalElements(), result.getTotalPages(), page, size);
    }

    @Transactional(readOnly = true)
    public Detail detail(Long id) {
        var user = viewer();
        var order = visibleOrder(id, user);
        var letter = snapshot(order);
        var visibleIds = visibility.assignedOnly(user) ? visibility.paperIds(user) : null;
        if (visibleIds != null && letter.get("executionPaper") instanceof Map<?, ?> execution) {
            // Snapshots were prepared by the head and may include links to other taxpayer papers.
            Map<Object, Object> safeExecution = new LinkedHashMap<>(execution);
            if (execution.get("otherActivePapers") instanceof List<?> links) safeExecution.put("otherActivePapers", links.stream()
                    .filter(link -> link instanceof Map<?, ?> map && map.get("id") instanceof Number number && visibleIds.contains(number.longValue())).toList());
            letter.put("executionPaper", safeExecution);
        }
        return new Detail(row(order, user), letter, events.findByArrestOrderIdOrderByIdAsc(id).stream()
                .map(event -> new Event(event.getId(), event.getAction(), event.getActorName(), event.getDestination(),
                        event.getOccurredAt(), event.getNote())).toList());
    }

    @Transactional(readOnly = true)
    public List<User> targets(Long paperId, Long orderId, BusinessRole role) {
        var user = viewer();
        if (role != DIRECTORATE_EXECUTION_TEAM_SUPERVISOR && role != EXECUTION_OFFICER) throw bad("نقش مقصد مجاز نیست.");
        ExecutionPaper paper;
        if (orderId != null) {
            var order = visibleOrder(orderId, user);
            boolean preparing = role == DIRECTORATE_EXECUTION_TEAM_SUPERVISOR && canPrepare(order, user);
            boolean assigning = role == EXECUTION_OFFICER && canAssign(order, user);
            if (!preparing && !assigning) throw denied();
            paper = order.getExecutionPaper();
        } else {
            requireRole(user, DIRECTORATE_EXECUTION_UNIT_HEAD);
            requirePermission(user, "ARREST_ORDER_START");
            paper = paper(paperId); requireScope(user, paper);
        }
        Long organization = directorate(paper);
        return profiles.findByBusinessRole(role).stream().filter(this::active)
                .filter(profile -> organization != null && Objects.equals(organization, profile.getGeneralDirectorateId()))
                .map(this::user).toList();
    }

    /** One paper per transaction: a failed row never rolls back other bulk results. */
    @Transactional
    public Row start(Long paperId, Start request) {
        // Reserve SQLite's writer before reads, also serializing concurrent duplicate starts.
        databaseWriter.lockIssuance();
        var actor = viewer(); requireRole(actor, DIRECTORATE_EXECUTION_UNIT_HEAD);
        requirePermission(actor, "ARREST_ORDER_START");
        var paper = paper(paperId); requireScope(actor, paper);
        if (orders.existsByExecutionPaperId(paperId)) throw conflict("برای این برگ اجرا قبلاً فرایند دستور بازداشت آغاز شده است.");
        requireEligible(paper);
        var order = new ArrestOrder(); order.setExecutionPaper(paper); order.setEngine(engine.name());
        order.setStartedAt(Instant.now()); order.setRoute(request.destination().name());
        if (request.destination() == Destination.SPECIAL_DEPARTMENT) {
            if (request.supervisorId() != null || request.officerId() != null) throw bad("در ارجاع به اداره ویژه، کاربر مقصد در مرحله بعد انتخاب می‌شود.");
            engine.advance(order, REFER_SPECIAL);
            order.setCandidateRole(SPECIAL_EXECUTION_DEPARTMENT_HEAD);
            refer(order, null, SPECIAL_TITLE);
        } else {
            if (request.destination() == Destination.OFFICER) {
                if (request.supervisorId() != null) throw bad("در مسیر مستقیم فقط مأمور اجرا را انتخاب کنید.");
                order.setOfficer(target(request.officerId(), EXECUTION_OFFICER, paper));
            } else {
                if (request.officerId() != null) throw bad("در مسیر سرپرست، مأمور اجرا در مرحله بعد انتخاب می‌شود.");
                order.setSupervisor(target(request.supervisorId(), DIRECTORATE_EXECUTION_TEAM_SUPERVISOR, paper));
            }
            order.setReviewer(actorProfile(actor));
            prepareLetter(order, actor); engine.advance(order, START);
            refer(order, order.getReviewer(), fullName(order.getReviewer()));
        }
        orders.saveAndFlush(order);
        record(order, "STARTED", actor, request.note());
        if (request.destination() == Destination.USER) { record(order, "SUPERVISOR_SELECTED", actor, fullName(order.getSupervisor())); recordPreparation(order, actor); }
        if (request.destination() == Destination.OFFICER) { record(order, "OFFICER_SELECTED", actor, fullName(order.getOfficer())); recordPreparation(order, actor); }
        return row(order, actor);
    }

    @Transactional
    public Row prepare(Long id, Transition request) {
        databaseWriter.lockIssuance();
        var actor = viewer(); var order = visibleOrder(id, actor); version(order, request);
        if (!canPrepare(order, actor)) throw denied();
        requireEligible(order.getExecutionPaper());
        order.setSupervisor(target(request.userId(), DIRECTORATE_EXECUTION_TEAM_SUPERVISOR, order.getExecutionPaper()));
        order.setReviewer(actorProfile(actor)); prepareLetter(order, actor);
        engine.advance(order, PREPARE); order.setCandidateRole(null);
        refer(order, order.getReviewer(), fullName(order.getReviewer()));
        orders.saveAndFlush(order); recordPreparation(order, actor);
        record(order, "SUPERVISOR_SELECTED", actor, request.note());
        return row(order, actor);
    }

    @Transactional
    public Row approve(Long id, Transition request) {
        databaseWriter.lockIssuance();
        var actor = viewer(); var order = visibleOrder(id, actor); version(order, request);
        if (!canApprove(order, actor)) throw denied();
        requireEligible(order.getExecutionPaper());
        if (debt(order.getExecutionPaper()).compareTo(new BigDecimal(text(snapshot(order).get("debtAmount")))) != 0)
            throw conflict("مبلغ بدهی تغییر کرده است؛ پیش‌نویس را بروزرسانی و دوباره بررسی کنید.");
        boolean direct = Destination.OFFICER.name().equals(order.getRoute());
        var recipient = direct ? order.getOfficer() : order.getSupervisor();
        target(recipient.getId(), direct ? EXECUTION_OFFICER : DIRECTORATE_EXECUTION_TEAM_SUPERVISOR, order.getExecutionPaper());
        engine.advance(order, direct ? APPROVE_TO_OFFICER : APPROVE);
        order.setIssuedAt(Instant.now());
        order.setLetterNumber(ArrestOrderNumbers.number(order.getId(), order.getIssuedAt()));
        var letter = snapshot(order); letter.put("approvedBy", actor.fullName());
        letter.put("approverRole", actor.businessRole() == null ? "مدیر سامانه" : actor.businessRole().getTitle());
        letter.put("issuedAt", order.getIssuedAt().toString()); letter.put("letterNumber", order.getLetterNumber());
        order.setLetterSnapshot(encode(letter));
        refer(order, recipient, fullName(recipient));
        var state = order.getExecutionPaper().getWorkflowState();
        state.setCurrentState("ARREST_ORDER_ISSUED"); workflowStates.save(state);
        orders.saveAndFlush(order); record(order, "APPROVED_AND_ISSUED", actor, request.note());
        record(order, "LETTER_ATTACHED", actor, "دستور بازداشت به برگ اجرا پیوست شد.");
        record(order, direct ? "ASSIGNED_TO_OFFICER" : "REFERRED_TO_SUPERVISOR", actor, null);
        return row(order, actor);
    }

    @Transactional
    public Row assignOfficer(Long id, Transition request) {
        databaseWriter.lockIssuance();
        var actor = viewer(); var order = visibleOrder(id, actor); version(order, request);
        if (!canAssign(order, actor)) throw denied();
        var officer = target(request.userId(), EXECUTION_OFFICER, order.getExecutionPaper());
        engine.advance(order, ASSIGN_OFFICER); order.setOfficer(officer);
        refer(order, officer, fullName(officer)); orders.saveAndFlush(order);
        record(order, "ASSIGNED_TO_OFFICER", actor, request.note());
        return row(order, actor);
    }

    @Transactional
    public Row refreshDraft(Long id, Transition request) {
        databaseWriter.lockIssuance();
        var actor = viewer(); var order = visibleOrder(id, actor); version(order, request);
        if (!canApprove(order, actor)) throw denied();
        requireEligible(order.getExecutionPaper());
        prepareLetter(order, actor); orders.saveAndFlush(order);
        record(order, "DRAFT_REFRESHED", actor, "پیش‌نویس از اطلاعات جاری برگ اجرا بروزرسانی شد.");
        return row(order, actor);
    }

    private void prepareLetter(ArrestOrder order, CurrentUserContext actor) {
        var detail = paperService.getDetails(order.getExecutionPaper().getId());
        // The only mocked integration: use the saved debt, without calling SANIM.
        BigDecimal amount = debt(order.getExecutionPaper());
        var letter = new LinkedHashMap<String, Object>();
        letter.put("templateVersion", "arrest-assets-v1");
        letter.put("executionPaper", detail.executionPaper()); letter.put("paper", detail.summary());
        letter.put("taxpayer", detail.taxpayer()); letter.put("finalTaxDebtPaper", detail.finalTaxDebtPaper());
        letter.put("notification", detail.notification());
        var notification = notifications.findByExecutionPaperId(order.getExecutionPaper().getId()).orElseThrow();
        letter.put("notificationViewDate", notification.getTaxpayerNotificationViewDate().toString());
        letter.put("paymentDeadline", notification.paymentDeadline().toString());
        letter.put("debtAmount", amount.toPlainString());
        BigDecimal surcharge = amount.multiply(new BigDecimal("0.10"));
        letter.put("surchargeRate", "10"); letter.put("surchargeAmount", surcharge.toPlainString());
        letter.put("totalAmount", amount.add(surcharge).toPlainString());
        var recipient = order.getOfficer() == null ? order.getSupervisor() : order.getOfficer();
        letter.put("supervisorName", fullName(recipient));
        letter.put("recipientName", fullName(recipient));
        letter.put("recipientRole", recipient.getBusinessRole().getTitle());
        letter.put("preparedBy", actor.fullName()); letter.put("issuerRole", actor.businessRole() == null ? "مدیر سامانه" : actor.businessRole().getTitle());
        letter.put("debtInquiryMode", "MOCK"); order.setDebtInquiredAt(Instant.now());
        letter.put("debtInquiredAt", order.getDebtInquiredAt().toString());
        order.setLetterSnapshot(encode(letter));
    }

    private void requireEligible(ExecutionPaper paper) {
        if (List.of("COMPLETED", "CANCELLED", "ARREST_ORDER_ISSUED").contains(paper.getCurrentState()))
            throw conflict("این برگ اجرا در وضعیت مجاز برای صدور دستور بازداشت نیست.");
        var notification = notifications.findByExecutionPaperId(paper.getId()).orElseThrow(() -> bad("برگ اجرا ابلاغ نشده است."));
        if (notification.getNotificationDate() == null || notification.paymentDeadline() == null
                || notification.paymentDeadline().isAfter(LocalDateTime.now(TEHRAN)))
            throw bad("مهلت پرداخت برگ اجرا هنوز منقضی نشده یا تاریخ رویت ابلاغ ثبت نشده است.");
        if (debt(paper).signum() <= 0) throw bad("برگ اجرا بدهی باقیمانده ندارد.");
    }

    private BigDecimal debt(ExecutionPaper paper) {
        return debts.findByExecutionPaperId(paper.getId()).map(DebtInformation::getExactOutstandingDebt)
                .orElseThrow(() -> bad("اطلاعات بدهی برگ اجرا موجود نیست."));
    }
    private CurrentUserContext viewer() { var user = identity.currentUser(); requirePermission(user, "ARREST_ORDER_VIEW"); return user; }
    private boolean national(CurrentUserContext user) { return user.isAdmin() || user.businessRole() != null && user.businessRole().isNationalScope(); }
    private Long directorate(CurrentUserContext user) { return user.organization() == null ? null : user.organization().generalDirectorateId(); }
    private Long directorate(ExecutionPaper paper) { var office = paper.getFinalTaxDebtPaper().getTaxAffairsOffice(); return office == null ? null : office.getGeneralDirectorateOfTaxAffairsId(); }
    private void requireScope(CurrentUserContext user, ExecutionPaper paper) {
        if (!national(user) && (directorate(user) == null || !Objects.equals(directorate(user), directorate(paper)))) throw denied();
    }
    private ExecutionPaper paper(Long id) { if (id == null) throw bad("برگ اجرا را انتخاب کنید."); return papers.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "برگ اجرا یافت نشد.")); }
    private ArrestOrder visibleOrder(Long id, CurrentUserContext user) { var order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "دستور بازداشت یافت نشد.")); requireScope(user, order.getExecutionPaper()); visibility.requireVisible(order.getExecutionPaper().getId()); return order; }
    private boolean canStart(CurrentUserContext user) { return user.hasPermission("ARREST_ORDER_START") && (user.isAdmin() || user.businessRole() == DIRECTORATE_EXECUTION_UNIT_HEAD); }

    public record WorkStats(long pending, long completed, long total, int percent) {}

    @Transactional(readOnly = true)
    public WorkStats workStats() {
        var user = viewer();
        var acted = new HashSet<>(events.findActedOrderIds(user.keycloakUserId(), user.username()));
        var responsible = new HashSet<Long>();
        long pending = 0;
        // Apply the same visibility and organization predicates as the task lists.
        for (int page = 0; ; page++) {
            var batch = list(page, 100, "", "", false, null);
            for (var row : batch.content()) {
                var order = orders.findById(row.id()).orElseThrow();
                boolean current = owned(order, user) || order.getAssignee() == null && order.getCandidateRole() == user.businessRole();
                boolean reviewed = order.getReviewer() != null && Objects.equals(order.getReviewer().getKeycloakUserId(), user.keycloakUserId());
                boolean assigned = visibility.assignedOnly(user);
                if (current || reviewed || assigned || acted.contains(order.getId())) responsible.add(order.getId());
                if (current && !row.allowedActions().isEmpty()) pending++;
            }
            if (page + 1 >= batch.totalPages()) break;
        }
        acted.retainAll(responsible);
        long ready = canStart(user) ? candidates(0, 1, "").totalElements() : 0;
        long total = responsible.size() + ready;
        return new WorkStats(pending + ready, acted.size(), total, total == 0 ? 0 : (int)Math.round(acted.size() * 100.0 / total));
    }
    private void requireRole(CurrentUserContext user, BusinessRole role) { if (!user.isAdmin() && user.businessRole() != role) throw denied(); }
    private void requirePermission(CurrentUserContext user, String permission) { if (!user.hasPermission(permission)) throw denied(); }
    private boolean owned(ArrestOrder order, CurrentUserContext user) { return order.getAssignee() != null && Objects.equals(order.getAssignee().getKeycloakUserId(), user.keycloakUserId()); }
    private boolean canPrepare(ArrestOrder order, CurrentUserContext user) { return order.getStage() == SPECIAL_DEPARTMENT_REVIEW && user.hasPermission("ARREST_ORDER_START") && (user.isAdmin() || user.businessRole() == SPECIAL_EXECUTION_DEPARTMENT_HEAD); }
    private boolean canApprove(ArrestOrder order, CurrentUserContext user) { return order.getStage() == AWAITING_APPROVAL && user.hasPermission("ARREST_ORDER_APPROVE") && (user.isAdmin() || owned(order, user) && (user.businessRole() == DIRECTORATE_EXECUTION_UNIT_HEAD || user.businessRole() == SPECIAL_EXECUTION_DEPARTMENT_HEAD)); }
    private boolean canAssign(ArrestOrder order, CurrentUserContext user) { return order.getStage() == AWAITING_OFFICER_ASSIGNMENT && user.hasPermission("ARREST_ORDER_ASSIGN") && (user.isAdmin() || owned(order, user) && user.businessRole() == DIRECTORATE_EXECUTION_TEAM_SUPERVISOR); }
    private AppUserProfile actorProfile(CurrentUserContext actor) { return profiles.findByKeycloakUserId(actor.keycloakUserId()).orElseThrow(() -> bad("پروفایل سازمانی کاربر ثبت نشده است.")); }
    private boolean active(AppUserProfile profile) { return Boolean.TRUE.equals(profile.getEnabledInBusinessSystem()) && !Boolean.TRUE.equals(profile.getLockedInBusinessSystem()); }
    private AppUserProfile target(Long id, BusinessRole role, ExecutionPaper paper) {
        if (id == null) throw bad("کاربر مقصد را انتخاب کنید.");
        var target = profiles.findById(id).orElseThrow(() -> bad("کاربر مقصد یافت نشد."));
        if (!active(target) || target.getBusinessRole() != role || directorate(paper) == null
                || !Objects.equals(directorate(paper), target.getGeneralDirectorateId())) throw bad("کاربر مقصد باید فعال، دارای نقش مجاز و متعلق به اداره کل برگ اجرا باشد.");
        return target;
    }
    private void version(ArrestOrder order, Transition request) { if (!Objects.equals(order.getVersion(), request.version())) throw conflict("این کار قبلاً تغییر کرده است؛ صفحه را بروزرسانی کنید."); }
    private void refer(ArrestOrder order, AppUserProfile assignee, String destination) { order.setAssignee(assignee); order.setReferralDestination(destination); order.setReferredAt(Instant.now()); }
    private void recordPreparation(ArrestOrder order, CurrentUserContext actor) { record(order, "DEBT_INQUIRY_MOCKED", actor, "استعلام بدهی آزمایشی با استفاده از آخرین بدهی ذخیره‌شده انجام شد."); record(order, "DRAFT_PREPARED", actor, null); }
    private void record(ArrestOrder order, String action, CurrentUserContext actor, String note) {
        var event = new ArrestOrderEvent(); event.setArrestOrder(order); event.setAction(action); event.setActorName(actor.fullName());
        event.setActorUsername(actor.username()); event.setDestination(order.getReferralDestination()); event.setOccurredAt(Instant.now());
        event.setNote(note); events.save(event);
        audit.record(actor, "ARREST_ORDER_" + action, "EXECUTION_PAPER", order.getExecutionPaper().getId().toString(), null,
                "order=" + order.getId() + "; destination=" + order.getReferralDestination());
    }
    private Row row(ArrestOrder order, CurrentUserContext actor) {
        var summary = paperService.getSummary(order.getExecutionPaper().getId());
        List<String> actions = new ArrayList<>(); if (canPrepare(order, actor)) actions.add("PREPARE");
        if (canApprove(order, actor)) actions.add("APPROVE"); if (canAssign(order, actor)) actions.add("ASSIGN_OFFICER");
        return new Row(order.getId(), order.getVersion(), order.getExecutionPaper().getId(), summary.uniqueCode(),
                summary.taxpayerName(), summary.taxpayerNationalId(),
                order.getLetterSnapshot() == null ? debt(order.getExecutionPaper()).toPlainString() : text(snapshot(order).get("debtAmount")),
                order.getStage().name(), order.getIssuedAt() == null ? "PENDING" : "ISSUED", order.getReferredAt(),
                order.getReferralDestination(), order.getAssignee() == null ? null : user(order.getAssignee()),
                order.getCandidateRole() == null ? null : order.getCandidateRole().name(), order.getStartedAt(), order.getIssuedAt(),
                order.getLetterNumber(), order.getEngine(), actions, order.getRoute());
    }
    private User user(AppUserProfile profile) { return new User(profile.getId(), profile.getUsername(), fullName(profile), profile.getBusinessRole().name(), profile.getBusinessRole().getTitle(), profile.getGeneralDirectorateId()); }
    private String fullName(AppUserProfile profile) { String name = (Objects.toString(profile.getFirstName(), "") + " " + Objects.toString(profile.getLastName(), "")).trim(); return name.isBlank() ? profile.getUsername() : name; }
    private Map<String, Object> snapshot(ArrestOrder order) { if (order.getLetterSnapshot() == null) return new LinkedHashMap<>(); try { return json.readValue(order.getLetterSnapshot(), new TypeReference<LinkedHashMap<String, Object>>() {}); } catch (Exception ex) { throw new IllegalStateException("Cannot read arrest letter snapshot", ex); } }
    private String encode(Object value) { try { return json.writeValueAsString(value); } catch (Exception ex) { throw new IllegalStateException("Cannot save arrest letter snapshot", ex); } }
    private PageRequest pagination(int page, int size) { if (page < 0 || size < 1 || size > 100) throw bad("اندازه صفحه باید بین ۱ تا ۱۰۰ باشد."); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String text(Object value) { return value == null ? "" : value.toString(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(CONFLICT, message); }
    private AccessDeniedException denied() { return new AccessDeniedException("دسترسی به این مرحله یا محدوده سازمانی مجاز نیست."); }
}
