package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.SanimAuditEntryRequest;
import com.mparsict.vosolmaliat.application.dto.response.*;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.integration.SanimAuditEntry;
import com.mparsict.vosolmaliat.domain.repository.SanimAuditEntryRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SanimAuditService {
    private final SanimAuditEntryRepository repository;
    private final TaxpayerRepository taxpayerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SanimTaxpayerOptionResponse> taxpayers() {
        Map<String, SanimTaxpayerOptionResponse> unique = new LinkedHashMap<>();
        taxpayerRepository.findAll().forEach(t -> {
            SanimTaxpayerOptionResponse option = taxpayerOption(t);
            unique.put(option.taxpayerType() + ":" + option.identifier(), option);
        });
        return List.copyOf(unique.values());
    }

    @Transactional
    public SanimAuditEntryResponse add(Long taxpayerId, SanimAuditEntryRequest request) {
        SanimTaxpayerOptionResponse taxpayer = taxpayerOption(requiredTaxpayer(taxpayerId));
        SanimAuditEntry saved = repository.save(SanimAuditEntry.builder()
                .taxpayerType(taxpayer.taxpayerType()).taxpayerIdentifier(taxpayer.identifier())
                .taxpayerDisplayName(taxpayer.displayName()).entryType(request.entryType())
                .amountRials(request.amountRials()).occurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt())
                .referenceNo(trim(request.referenceNo())).description(trim(request.description())).build());
        auditLogService.record(null, "SANIM_AUDIT_ENTRY_CREATED", "TAXPAYER", taxpayer.identifier(), null,
                saved.getEntryType() + ":" + saved.getAmountRials());
        return response(saved);
    }

    @Transactional
    public List<SanimAuditEntryResponse> generateRandom(Long taxpayerId, int count) {
        List<SanimAuditEntryResponse> created = new ArrayList<>();
        SanimAuditEntry.EntryType[] types = SanimAuditEntry.EntryType.values();
        for (int i = 0; i < count; i++) {
            var random = ThreadLocalRandom.current();
            created.add(add(taxpayerId, new SanimAuditEntryRequest(
                    i < types.length ? types[i] : types[random.nextInt(types.length)], random.nextLong(100_000L, 50_000_001L),
                    LocalDateTime.now().minusDays(random.nextInt(0, 366)), "RND-" + UUID.randomUUID().toString().substring(0, 8),
                    "داده آزمایشی تولیدشده توسط سامانه"
            )));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<SanimAuditEntryResponse> history(Long taxpayerId) {
        SanimTaxpayerOptionResponse taxpayer = taxpayerOption(requiredTaxpayer(taxpayerId));
        return entries(taxpayer.taxpayerType(), taxpayer.identifier()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SanimDebtStatusResponse inquire(Long taxpayerId) {
        SanimTaxpayerOptionResponse taxpayer = taxpayer(taxpayerId);
        return inquire(taxpayer.taxpayerType(), taxpayer.identifier());
    }

    @Transactional(readOnly = true)
    public SanimTaxpayerOptionResponse taxpayer(Long taxpayerId) {
        return taxpayerOption(requiredTaxpayer(taxpayerId));
    }

    @Transactional(readOnly = true)
    public SanimDebtStatusResponse inquire(TaxpayerType type, String identifier) {
        List<SanimAuditEntry> entries = entries(type, identifier);
        if (entries.isEmpty()) throw new BusinessException("SANIM_AUDIT_NOT_FOUND", "سابقه بدهی مؤدی در سنیم یافت نشد");
        long debt = sum(entries, SanimAuditEntry.EntryType.DEBT);
        long penalty = sum(entries, SanimAuditEntry.EntryType.PENALTY);
        long payment = sum(entries, SanimAuditEntry.EntryType.PAYMENT);
        long credit = sum(entries, SanimAuditEntry.EntryType.CREDIT);
        SanimAuditEntry latest = entries.getFirst();
        return new SanimDebtStatusResponse(type, identifier, latest.getTaxpayerDisplayName(), debt, penalty, payment, credit,
                Math.max(0L, debt + penalty - payment - credit), entries.size(), latest.getOccurredAt());
    }

    private List<SanimAuditEntry> entries(TaxpayerType type, String identifier) {
        if (type == null || identifier == null || identifier.isBlank())
            throw new BusinessException("SANIM_TAXPAYER_IDENTIFIER_REQUIRED", "نوع و شناسه مؤدی الزامی است");
        return repository.findAllByTaxpayerTypeAndTaxpayerIdentifierOrderByOccurredAtDescIdDesc(type, identifier.trim());
    }
    private long sum(List<SanimAuditEntry> entries, SanimAuditEntry.EntryType type) {
        return entries.stream().filter(e -> e.getEntryType() == type).mapToLong(SanimAuditEntry::getAmountRials).sum();
    }
    private Taxpayer requiredTaxpayer(Long id) { return taxpayerRepository.findById(id)
            .orElseThrow(() -> new BusinessException("TAXPAYER_NOT_FOUND", "مؤدی یافت نشد")); }
    private SanimTaxpayerOptionResponse taxpayerOption(Taxpayer t) {
        if (t.getNaturalPerson() != null && t.getNaturalPerson().getNationalCode() != null)
            return new SanimTaxpayerOptionResponse(t.getId(), TaxpayerType.NATURAL, t.getNaturalPerson().getNationalCode(),
                    t.getNaturalPerson().getFirstName() + " " + t.getNaturalPerson().getLastName());
        if (t.getLegalPerson() != null && t.getLegalPerson().getNationalId() != null)
            return new SanimTaxpayerOptionResponse(t.getId(), TaxpayerType.LEGAL, t.getLegalPerson().getNationalId(), t.getLegalPerson().getName());
        throw new BusinessException("TAXPAYER_IDENTIFIER_MISSING", "شناسه ملی/کد ملی مؤدی موجود نیست");
    }
    private SanimAuditEntryResponse response(SanimAuditEntry e) { return new SanimAuditEntryResponse(e.getId(), e.getTaxpayerType(),
            e.getTaxpayerIdentifier(), e.getTaxpayerDisplayName(), e.getEntryType(), e.getAmountRials(), e.getOccurredAt(),
            e.getReferenceNo(), e.getDescription(), e.getCreatedAt()); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
