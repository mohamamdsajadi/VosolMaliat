package com.mparsict.vosolmaliat.application.service.masterdata;

import com.mparsict.vosolmaliat.application.dto.request.masterdata.GeneralDirectorateRequest;
import com.mparsict.vosolmaliat.application.dto.request.masterdata.TaxAffairsOfficeRequest;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.GeneralDirectorateResponse;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.TaxAffairsOfficeResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairsHistory;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOfficeHistory;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsRepository;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsHistoryRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterdataService {

    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorateRepository;
    private final TaxAffairsOfficeRepository taxAffairsOfficeRepository;
    private final GeneralDirectorateOfTaxAffairsHistoryRepository generalDirectorateHistoryRepository;
    private final TaxAffairsOfficeHistoryRepository taxAffairsOfficeHistoryRepository;

    @Transactional(readOnly = true)
    public List<GeneralDirectorateResponse> listGeneralDirectorates() {
        return generalDirectorateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public GeneralDirectorateResponse createGeneralDirectorate(GeneralDirectorateRequest request) {
        GeneralDirectorateOfTaxAffairs saved = generalDirectorateRepository.save(GeneralDirectorateOfTaxAffairs.builder()
                .generalDirectorateName(request.name())
                .postalCode(request.postalCode())
                .address(request.address())
                .phone(request.phone())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public GeneralDirectorateResponse updateGeneralDirectorate(Long id, GeneralDirectorateRequest request) {
        GeneralDirectorateOfTaxAffairs existing = findGeneralDirectorate(id);
        generalDirectorateHistoryRepository.save(
                GeneralDirectorateOfTaxAffairsHistory.from(existing, "UPDATE"));
        existing.setGeneralDirectorateName(request.name());
        existing.setPostalCode(request.postalCode());
        existing.setAddress(request.address());
        existing.setPhone(request.phone());
        return toResponse(generalDirectorateRepository.save(existing));
    }

    @Transactional
    public void deleteGeneralDirectorate(Long id) {
        GeneralDirectorateOfTaxAffairs existing = findGeneralDirectorate(id);
        generalDirectorateHistoryRepository.save(
                GeneralDirectorateOfTaxAffairsHistory.from(existing, "DELETE"));
        generalDirectorateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TaxAffairsOfficeResponse> listTaxAffairsOffices(Long generalDirectorateId) {
        List<TaxAffairsOffice> offices = generalDirectorateId == null
                ? taxAffairsOfficeRepository.findAll()
                : taxAffairsOfficeRepository.findByGeneralDirectorateOfTaxAffairsId(generalDirectorateId);
        return offices.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaxAffairsOfficeResponse createTaxAffairsOffice(TaxAffairsOfficeRequest request) {
        GeneralDirectorateOfTaxAffairs general = findGeneralDirectorate(request.generalDirectorateOfTaxAffairsId());
        TaxAffairsOffice saved = taxAffairsOfficeRepository.save(TaxAffairsOffice.builder()
                .generalDirectorateOfTaxAffairs(general)
                .officeName(request.officeName())
                .postalCode(request.postalCode())
                .address(request.address())
                .phone(request.phone())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public TaxAffairsOfficeResponse updateTaxAffairsOffice(Long id, TaxAffairsOfficeRequest request) {
        GeneralDirectorateOfTaxAffairs general = findGeneralDirectorate(request.generalDirectorateOfTaxAffairsId());
        TaxAffairsOffice existing = findTaxAffairsOffice(id);
        taxAffairsOfficeHistoryRepository.save(TaxAffairsOfficeHistory.from(existing, "UPDATE"));
        existing.setGeneralDirectorateOfTaxAffairs(general);
        existing.setOfficeName(request.officeName());
        existing.setPostalCode(request.postalCode());
        existing.setAddress(request.address());
        existing.setPhone(request.phone());
        return toResponse(taxAffairsOfficeRepository.save(existing));
    }

    @Transactional
    public void deleteTaxAffairsOffice(Long id) {
        TaxAffairsOffice existing = findTaxAffairsOffice(id);
        taxAffairsOfficeHistoryRepository.save(TaxAffairsOfficeHistory.from(existing, "DELETE"));
        taxAffairsOfficeRepository.deleteById(id);
    }

    private GeneralDirectorateOfTaxAffairs findGeneralDirectorate(Long id) {
        return generalDirectorateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GENERAL_DIRECTORATE_NOT_FOUND", "اداره کل امور مالیاتی یافت نشد"));
    }

    private TaxAffairsOffice findTaxAffairsOffice(Long id) {
        return taxAffairsOfficeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("TAX_AFFAIRS_OFFICE_NOT_FOUND", "اداره مالیات یافت نشد"));
    }

    private GeneralDirectorateResponse toResponse(GeneralDirectorateOfTaxAffairs entity) {
        return new GeneralDirectorateResponse(
                entity.getId(),
                entity.getGeneralDirectorateName(),
                entity.getPostalCode(),
                entity.getAddress(),
                entity.getPhone()
        );
    }

    private TaxAffairsOfficeResponse toResponse(TaxAffairsOffice entity) {
        GeneralDirectorateOfTaxAffairs general = generalDirectorateRepository
                .findById(entity.getGeneralDirectorateOfTaxAffairsId())
                .orElse(null);
        return new TaxAffairsOfficeResponse(
                entity.getId(),
                entity.getGeneralDirectorateOfTaxAffairsId(),
                general == null ? null : general.getGeneralDirectorateName(),
                entity.getOfficeName(),
                entity.getPostalCode(),
                entity.getAddress(),
                entity.getPhone(), null, null
        );
    }

}
