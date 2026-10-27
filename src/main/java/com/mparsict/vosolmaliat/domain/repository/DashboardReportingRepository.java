package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.application.dto.response.DashboardPaperRow;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface DashboardReportingRepository extends Repository<ExecutionPaper, Long> {
    @Query("""
        select new com.mparsict.vosolmaliat.application.dto.response.DashboardPaperRow(
            ep.id, ep.executionPaperUniqueCode, ep.issuanceDate, tp.id, tp.tin,
            coalesce(li.name, trim(concat(coalesce(ni.firstName, ''), ' ', coalesce(ni.lastName, '')))),
            coalesce(lp.nationalId, np.nationalCode), ep.workflowState.currentState,
            case when n.notificationDate is not null then true else false end,
            gd.id, gd.generalDirectorateName, office.id, office.officeName,
            d.id, d.totalLiability, d.totalPenalties, d.totalCredits, d.totalPayments,
            d.totalRemissions, d.outstandingTaxDebt, arrest.issuedAt)
        from ExecutionPaper ep
        join ep.finalTaxDebtPaper fp join fp.taxpayer tp
        left join tp.naturalPerson np left join np.extraInfo ni
        left join tp.legalPerson lp left join lp.extraInfo li
        left join fp.taxAffairsOffice office left join office.generalDirectorateOfTaxAffairs gd
        left join DebtInformation d on d.finalTaxDebtPaper = fp
        left join ExecutionPaperNotification n on n.executionPaper = ep
        left join ArrestOrder arrest on arrest.executionPaper = ep
        where (:restrictAssignments = false or ep.id in :paperIds)
          and (:restrictOrganization = false or gd.id = :directorateId)
        order by ep.id desc
        """)
    List<DashboardPaperRow> currentRows(
            @Param("restrictAssignments") boolean restrictAssignments,
            @Param("paperIds") Collection<Long> paperIds,
            @Param("restrictOrganization") boolean restrictOrganization,
            @Param("directorateId") Long directorateId);
}
