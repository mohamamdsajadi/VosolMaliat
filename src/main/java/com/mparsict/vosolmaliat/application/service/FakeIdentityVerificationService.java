package com.mparsict.vosolmaliat.application.service;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.integration.FakeIdentityVerification;
import com.mparsict.vosolmaliat.domain.repository.FakeIdentityVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class FakeIdentityVerificationService {
    private final FakeIdentityVerificationRepository repository;
    private final com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator codes;
    public boolean applies(DebtPaperIssuanceRequest request) {
        return request.sourceDocument()!=null && "LTR_EF03".equals(request.sourceDocument().getTemplateCode());
    }
    @Transactional
    public String status(DebtPaperIssuanceRequest request) {
        if (!applies(request)) return "VERIFIED";
        String identifier=request.sourceDocument().getIdentityNumber();
        var profile=repository.findById(identifier).orElse(null);
        if (profile==null) {
            repository.save(FakeIdentityVerification.builder().identifier(identifier)
                    .personType(request.taxpayer().type()).status("PENDING").build());
            return "PENDING";
        }
        if (!profile.getPersonType().equals(request.taxpayer().type()))
            throw new BusinessException("IDENTITY_CONFLICT","نوع شخص با پاسخ استعلام هویت سازگار نیست");
        return profile.getStatus();
    }
    @Transactional(readOnly=true)
    public DebtPaperIssuanceRequest.TaxpayerInfo verifiedPerson(DebtPaperIssuanceRequest request) {
        if (!applies(request)) return request.taxpayer();
        var p=repository.findById(request.sourceDocument().getIdentityNumber()).orElseThrow(
                () -> new BusinessException("IDENTITY_PENDING","در انتظار تأیید هویت"));
        if (!"VERIFIED".equals(p.getStatus())) throw new BusinessException("IDENTITY_"+p.getStatus(),"صدور نیازمند تأیید هویت است");
        var source=request.taxpayer();
        String sourceIdentifier="NATURAL".equals(source.type()) ? (source.nationalCode() == null ? source.fida() : source.nationalCode()) : source.nationalId();
        if (!p.getIdentifier().equals(sourceIdentifier)) throw new BusinessException("IDENTITY_CONFLICT","شناسه شخص با پاسخ استعلام سازگار نیست");
        if (!p.getPersonType().equals(source.type())) throw new BusinessException("IDENTITY_CONFLICT","نوع شخص ناسازگار است");
        return new DebtPaperIssuanceRequest.TaxpayerInfo(p.getPersonType(), source.nationalCode(), source.fida(),
                p.getFirstName(),p.getLastName(),p.getFatherName(),p.getBirthDate(),source.postalCode(),source.address(),
                source.nationalId(),p.getRegistrationNumber(),p.getRegistrationDate(),p.getCompanyName());
    }
    @Transactional
    public void decide(String identifier, Decision decision) {
        codes.lockIssuance();
        var profile=repository.findById(identifier).orElseThrow(() -> new BusinessException("IDENTITY_NOT_FOUND","استعلام هویت یافت نشد"));
        if (decision.verified()) {
            if ("NATURAL".equals(profile.getPersonType()) && (blank(decision.firstName()) || blank(decision.lastName())))
                throw new BusinessException("IDENTITY_NAME_REQUIRED","نام و نام خانوادگی تأییدشده لازم است");
            if ("LEGAL".equals(profile.getPersonType()) && blank(decision.companyName()))
                throw new BusinessException("IDENTITY_NAME_REQUIRED","نام تأییدشده شرکت لازم است");
            profile.setStatus("VERIFIED"); profile.setRejectionReason(null);
            profile.setFirstName(decision.firstName()); profile.setLastName(decision.lastName());
            profile.setFatherName(decision.fatherName()); profile.setBirthDate(decision.birthDate());
            profile.setCompanyName(decision.companyName()); profile.setRegistrationNumber(decision.registrationNumber());
            profile.setRegistrationDate(decision.registrationDate());
        } else {
            if (blank(decision.reason())) throw new BusinessException("REJECTION_REASON_REQUIRED","علت رد هویت لازم است");
            profile.setStatus("REJECTED"); profile.setRejectionReason(decision.reason());
        }
        repository.save(profile);
    }
    public String rejectionReason(String identifier) {
        return repository.findById(identifier).map(FakeIdentityVerification::getRejectionReason).orElse("هویت تأیید نشد");
    }
    private boolean blank(String value) { return value==null || value.isBlank(); }
    public record Decision(boolean verified,String firstName,String lastName,String fatherName,String birthDate,
                           String companyName,String registrationNumber,String registrationDate,String reason) {}
}
