package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.response.DashboardOverviewResponse;
import com.mparsict.vosolmaliat.application.dto.response.DashboardPaperRow;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.repository.DashboardReportingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import static com.mparsict.vosolmaliat.application.dto.response.DashboardOverviewResponse.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class DashboardReportingService {
    private final DashboardReportingRepository repository;
    private final SecurityIdentityService identity;
    private final ExecutionPaperVisibility visibility;

    public DashboardOverviewResponse overview() {
        var user = identity.currentUser();
        var rows = rows(user);
        var balances = new Balances(sum(rows, DashboardPaperRow::liability), sum(rows, DashboardPaperRow::penalties),
                sum(rows, DashboardPaperRow::credits), sum(rows, DashboardPaperRow::paid),
                sum(rows, DashboardPaperRow::remissions), sum(rows, DashboardPaperRow::remaining));
        var states = Arrays.stream(ExecutionPaperState.values()).map(state ->
                bucket(state.name(), state.getPersianLabel(), select(rows, row -> state.name().equals(row.state())))).toList();
        var notifications = List.of(bucket("NOTIFIED", "ابلاغ‌شده", select(rows, DashboardPaperRow::notified)),
                bucket("NOT_NOTIFIED", "ابلاغ‌نشده", select(rows, row -> !row.notified())));
        // Local roles compare tax offices; national roles compare general directorates.
        boolean national = national(user);
        Map<Long, List<DashboardPaperRow>> offices = new HashMap<>();
        for (var row : rows) offices.computeIfAbsent(officeKey(row, national), key -> new ArrayList<>()).add(row);
        var officeBuckets = offices.entrySet().stream().map(entry -> {
            var first = entry.getValue().getFirst();
            String label = national ? first.directorateName() : first.officeName();
            return bucket(entry.getKey().toString(), label == null ? "واحد نامشخص" : label, entry.getValue());
        }).sorted(Comparator.comparing(Bucket::count).reversed().thenComparing(Bucket::key)).limit(10).toList();
        var enforcement = List.of(bucket("PAPERS", "برگ‌های اجرا", rows),
                bucket("ISSUED", "دستور بازداشت صادر شده", select(rows, row -> row.orderIssuedAt() != null)),
                new Bucket("SEIZURE", "توقیف شده", null, null, null, false),
                new Bucket("AUCTION", "فروش رفته", null, null, null, false));
        String scope = visibility.assignedOnly(user) ? "برگ‌های ارجاع‌شده به شما"
                : national ? "همه اداره‌های کل قابل مشاهده"
                : user.organization() == null || user.organization().generalDirectorateName() == null
                    ? "اداره کل منتسب به حساب شما" : user.organization().generalDirectorateName();
        return new DashboardOverviewResponse(scope, rows.stream().map(DashboardPaperRow::taxpayerId).distinct().count(),
                rows.size(), balances, states, notifications, officeBuckets, enforcement,
                user.hasPermission("REPORT_VIEW") || user.hasPermission("EXECUTION_PAPER_LIST_VIEW"));
    }

    public Details details(String group, String key, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw invalid();
        var user = identity.currentUser();
        Function<DashboardPaperRow, BigDecimal> amount = "finance".equals(group) ? financialField(key) : DashboardPaperRow::remaining;
        Predicate<DashboardPaperRow> predicate = switch (group) {
            case "all" -> { if (!"PAPERS".equals(key) && !"TAXPAYERS".equals(key)) throw invalid(); yield row -> true; }
            case "state" -> {
                try { ExecutionPaperState.valueOf(key); } catch (IllegalArgumentException exception) { throw invalid(); }
                yield row -> key.equals(row.state());
            }
            case "notification" -> {
                if (!List.of("NOTIFIED", "NOT_NOTIFIED").contains(key)) throw invalid();
                yield row -> row.notified() == "NOTIFIED".equals(key);
            }
            case "office" -> {
                long id; try { id = Long.parseLong(key); } catch (NumberFormatException exception) { throw invalid(); }
                yield row -> officeKey(row, national(user)) == id;
            }
            case "finance" -> row -> amount.apply(row) == null || amount.apply(row).signum() != 0;
            case "enforcement" -> {
                if (!List.of("PAPERS", "ISSUED", "NOT_ISSUED").contains(key)) throw invalid();
                yield row -> "PAPERS".equals(key) || ("ISSUED".equals(key) == (row.orderIssuedAt() != null));
            }
            default -> throw invalid();
        };
        var selected = select(rows(user), predicate);
        long offset = (long) page * size;
        var countedDebts = new HashSet<Long>();
        var content = selected.stream().map(row -> new Detail(row.id(), row.uniqueCode(),
                row.issuanceDate(), row.taxpayerCode(), row.taxpayerName(), row.nationalId(),
                ExecutionPaperState.labelOf(row.state()), row.officeName(), decimal(row.remaining()),
                decimal(row.debtId() == null ? null : countedDebts.add(row.debtId()) ? amount.apply(row) : BigDecimal.ZERO)))
                .skip(offset).limit(size).toList();
        return new Details(content, selected.size(), (selected.size() + size - 1) / size, page, size, sum(selected, amount));
    }

    private List<DashboardPaperRow> rows(CurrentUserContext user) {
        boolean assigned = visibility.assignedOnly(user);
        var ids = assigned ? visibility.paperIds(user) : List.<Long>of();
        Long directorate = user.organization() == null ? null : user.organization().generalDirectorateId();
        return repository.currentRows(assigned, ids.isEmpty() ? List.of(-1L) : ids, !national(user),
                directorate == null ? -1L : directorate);
    }

    private static boolean national(CurrentUserContext user) {
        return user.isAdmin() || user.businessRole() != null && user.businessRole().isNationalScope();
    }
    private static long officeKey(DashboardPaperRow row, boolean national) {
        Long id = national ? row.directorateId() : row.officeId();
        return id == null ? -1L : id;
    }
    private static List<DashboardPaperRow> select(List<DashboardPaperRow> rows, Predicate<DashboardPaperRow> predicate) {
        return rows.stream().filter(predicate).toList();
    }
    private static Bucket bucket(String key, String label, List<DashboardPaperRow> rows) {
        return new Bucket(key, label, (long) rows.size(), sum(rows, DashboardPaperRow::remaining), sum(rows, DashboardPaperRow::paid), true);
    }
    private static Function<DashboardPaperRow, BigDecimal> financialField(String key) {
        return switch (key) {
            case "liability" -> DashboardPaperRow::liability;
            case "penalties" -> DashboardPaperRow::penalties;
            case "credits" -> DashboardPaperRow::credits;
            case "paid" -> DashboardPaperRow::paid;
            case "remissions" -> DashboardPaperRow::remissions;
            case "remaining" -> DashboardPaperRow::remaining;
            default -> throw invalid();
        };
    }
    private static String sum(List<DashboardPaperRow> rows, Function<DashboardPaperRow, BigDecimal> field) {
        BigDecimal total = BigDecimal.ZERO;
        var countedDebts = new HashSet<Long>();
        for (var row : rows) {
            var value = field.apply(row);
            if (value == null) return null;
            if (!countedDebts.add(row.debtId())) continue;
            total = total.add(value);
        }
        return decimal(total);
    }
    private static String decimal(BigDecimal value) { return value == null ? null : value.toPlainString(); }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "گروه، مقدار فیلتر یا صفحه‌بندی معتبر نیست.");
    }
}
