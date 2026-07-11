package com.mparsict.vosolmaliat.presentation.api.masterdata;

import com.mparsict.vosolmaliat.application.dto.request.masterdata.GeneralDirectorateRequest;
import com.mparsict.vosolmaliat.application.dto.request.masterdata.TaxAffairsOfficeRequest;
import com.mparsict.vosolmaliat.application.dto.request.masterdata.TaxUnitRequest;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.GeneralDirectorateResponse;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.TaxAffairsOfficeResponse;
import com.mparsict.vosolmaliat.application.dto.response.masterdata.TaxUnitResponse;
import com.mparsict.vosolmaliat.application.service.masterdata.MasterdataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/masterdata")
@RequiredArgsConstructor
public class MasterdataController {

    private final MasterdataService masterdataService;

    @GetMapping("/general-directorates")
    @PreAuthorize("hasAnyAuthority('USER_PROFILE_VIEW','USER_PROFILE_ASSIGN_ORGANIZATION')")
    public List<GeneralDirectorateResponse> listGeneralDirectorates() {
        return masterdataService.listGeneralDirectorates();
    }

    @PostMapping("/general-directorates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public GeneralDirectorateResponse createGeneralDirectorate(@RequestBody @Valid GeneralDirectorateRequest request) {
        return masterdataService.createGeneralDirectorate(request);
    }

    @PutMapping("/general-directorates/{id}")
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public GeneralDirectorateResponse updateGeneralDirectorate(@PathVariable Long id, @RequestBody @Valid GeneralDirectorateRequest request) {
        return masterdataService.updateGeneralDirectorate(id, request);
    }

    @DeleteMapping("/general-directorates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public void deleteGeneralDirectorate(@PathVariable Long id) {
        masterdataService.deleteGeneralDirectorate(id);
    }

    @GetMapping("/tax-affairs-offices")
    @PreAuthorize("hasAnyAuthority('USER_PROFILE_VIEW','USER_PROFILE_ASSIGN_ORGANIZATION')")
    public List<TaxAffairsOfficeResponse> listTaxAffairsOffices(@RequestParam(required = false) Long generalDirectorateId) {
        return masterdataService.listTaxAffairsOffices(generalDirectorateId);
    }

    @PostMapping("/tax-affairs-offices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public TaxAffairsOfficeResponse createTaxAffairsOffice(@RequestBody @Valid TaxAffairsOfficeRequest request) {
        return masterdataService.createTaxAffairsOffice(request);
    }

    @PutMapping("/tax-affairs-offices/{id}")
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public TaxAffairsOfficeResponse updateTaxAffairsOffice(@PathVariable Long id, @RequestBody @Valid TaxAffairsOfficeRequest request) {
        return masterdataService.updateTaxAffairsOffice(id, request);
    }

    @DeleteMapping("/tax-affairs-offices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public void deleteTaxAffairsOffice(@PathVariable Long id) {
        masterdataService.deleteTaxAffairsOffice(id);
    }

    @GetMapping("/tax-units")
    @PreAuthorize("hasAnyAuthority('USER_PROFILE_VIEW','USER_PROFILE_ASSIGN_ORGANIZATION')")
    public List<TaxUnitResponse> listTaxUnits(@RequestParam(required = false) Long taxAffairsOfficeId) {
        return masterdataService.listTaxUnits(taxAffairsOfficeId);
    }

    @PostMapping("/tax-units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public TaxUnitResponse createTaxUnit(@RequestBody @Valid TaxUnitRequest request) {
        return masterdataService.createTaxUnit(request);
    }

    @PutMapping("/tax-units/{id}")
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public TaxUnitResponse updateTaxUnit(@PathVariable Long id, @RequestBody @Valid TaxUnitRequest request) {
        return masterdataService.updateTaxUnit(id, request);
    }

    @DeleteMapping("/tax-units/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
    public void deleteTaxUnit(@PathVariable Long id) {
        masterdataService.deleteTaxUnit(id);
    }
}
