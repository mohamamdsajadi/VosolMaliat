package com.mparsict.vosolmaliat.application.service.masterdata;

import com.mparsict.vosolmaliat.application.dto.request.masterdata.GeneralDirectorateRequest;
import com.mparsict.vosolmaliat.application.dto.request.masterdata.TaxAffairsOfficeRequest;
import com.mparsict.vosolmaliat.application.dto.request.masterdata.TaxUnitRequest;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.GeneralDirectorateResponse;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.TaxAffairsOfficeResponse;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.TaxUnitResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterdataService {

    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorateRepository;
    private final TaxAffairsOfficeRepository taxAffairsOfficeRepository;
    private final TaxUnitRepository taxUnitRepository;

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
        existing.setGeneralDirectorateName(request.name());
        existing.setPostalCode(request.postalCode());
        existing.setAddress(request.address());
        existing.setPhone(request.phone());
        return toResponse(generalDirectorateRepository.save(existing));
    }

    @Transactional
    public void deleteGeneralDirectorate(Long id) {
        if (!generalDirectorateRepository.existsById(id)) {
            throw new BusinessException("GENERAL_DIRECTORATE_NOT_FOUND", "اداره کل امور مالیاتی یافت نشد");
        }
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
        findGeneralDirectorate(request.generalDirectorateOfTaxAffairsId());
        TaxAffairsOffice saved = taxAffairsOfficeRepository.save(TaxAffairsOffice.builder()
                .generalDirectorateOfTaxAffairsId(request.generalDirectorateOfTaxAffairsId())
                .officeName(request.officeName())
                .postalCode(request.postalCode())
                .address(request.address())
                .phone(request.phone())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public TaxAffairsOfficeResponse updateTaxAffairsOffice(Long id, TaxAffairsOfficeRequest request) {
        findGeneralDirectorate(request.generalDirectorateOfTaxAffairsId());
        TaxAffairsOffice existing = findTaxAffairsOffice(id);
        existing.setGeneralDirectorateOfTaxAffairsId(request.generalDirectorateOfTaxAffairsId());
        existing.setOfficeName(request.officeName());
        existing.setPostalCode(request.postalCode());
        existing.setAddress(request.address());
        existing.setPhone(request.phone());
        return toResponse(taxAffairsOfficeRepository.save(existing));
    }

    @Transactional
    public void deleteTaxAffairsOffice(Long id) {
        if (!taxAffairsOfficeRepository.existsById(id)) {
            throw new BusinessException("TAX_AFFAIRS_OFFICE_NOT_FOUND", "اداره مالیات یافت نشد");
        }
        taxAffairsOfficeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TaxUnitResponse> listTaxUnits(Long taxAffairsOfficeId) {
        List<TaxUnit> units = taxAffairsOfficeId == null
                ? taxUnitRepository.findAll()
                : taxUnitRepository.findByTaxAffairsOfficeId(taxAffairsOfficeId);
        return units.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaxUnitResponse createTaxUnit(TaxUnitRequest request) {
        findTaxAffairsOffice(request.taxAffairsOfficeId());
        TaxUnit saved = taxUnitRepository.save(TaxUnit.builder()
                .taxAffairsOfficeId(request.taxAffairsOfficeId())
                .taxUnitName(request.taxUnitName())
                .postalCode(request.postalCode())
                .address(request.address())
                .phone(request.phone())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public TaxUnitResponse updateTaxUnit(Long id, TaxUnitRequest request) {
        findTaxAffairsOffice(request.taxAffairsOfficeId());
        TaxUnit existing = findTaxUnit(id);
        existing.setTaxAffairsOfficeId(request.taxAffairsOfficeId());
        existing.setTaxUnitName(request.taxUnitName());
        existing.setPostalCode(request.postalCode());
        existing.setAddress(request.address());
        existing.setPhone(request.phone());
        return toResponse(taxUnitRepository.save(existing));
    }

    @Transactional
    public void deleteTaxUnit(Long id) {
        if (!taxUnitRepository.existsById(id)) {
            throw new BusinessException("TAX_UNIT_NOT_FOUND", "واحد مالیاتی یافت نشد");
        }
        taxUnitRepository.deleteById(id);
    }

    private GeneralDirectorateOfTaxAffairs findGeneralDirectorate(Long id) {
        return generalDirectorateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GENERAL_DIRECTORATE_NOT_FOUND", "اداره کل امور مالیاتی یافت نشد"));
    }

    private TaxAffairsOffice findTaxAffairsOffice(Long id) {
        return taxAffairsOfficeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("TAX_AFFAIRS_OFFICE_NOT_FOUND", "اداره مالیات یافت نشد"));
    }

    private TaxUnit findTaxUnit(Long id) {
        return taxUnitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("TAX_UNIT_NOT_FOUND", "واحد مالیاتی یافت نشد"));
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
                entity.getPhone()
        );
    }

    private TaxUnitResponse toResponse(TaxUnit entity) {
        TaxAffairsOffice office = taxAffairsOfficeRepository.findById(entity.getTaxAffairsOfficeId()).orElse(null);
        GeneralDirectorateOfTaxAffairs general = office == null ? null : generalDirectorateRepository
                .findById(office.getGeneralDirectorateOfTaxAffairsId())
                .orElse(null);
        return new TaxUnitResponse(
                entity.getId(),
                entity.getTaxAffairsOfficeId(),
                office == null ? null : office.getOfficeName(),
                office == null ? null : office.getGeneralDirectorateOfTaxAffairsId(),
                general == null ? null : general.getGeneralDirectorateName(),
                entity.getTaxUnitName(),
                entity.getPostalCode(),
                entity.getAddress(),
                entity.getPhone()
        );
    }
}
