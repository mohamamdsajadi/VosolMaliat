package com.mparsict.vosolmaliat.application.service;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class Ef03ReceptionService {
    private final DebtPaperXmlReader reader;
    private final DebtPaperPushService push;
    private final DebtPaperPushRecordRepository receipts;
    private final ExecutionPaperRepository papers;
    public Result receive(String xml,String reference,String followCode,String officeCode,String taxSource) {
        var values=new SanimXmlValues(reader.parse(xml));
        officeCode=resolveOfficeCode(values.text("I"),officeCode);
        int year;
        try { year=Integer.parseInt(SanimXmlValues.digits(values.required("C"))); }
        catch(NumberFormatException ex) { throw SanimXmlValues.invalid("Invalid XML tax year"); }
        Long id=push.push(new DebtPaperPushRequest(SanimXmlValues.digits(values.required("R")),xml,null,OffsetDateTime.now(),
                reference,followCode,null,year,1,officeCode,null,taxSource,"LTR_EF03"),"admin-"+UUID.randomUUID());
        var receipt=receipts.findByReferenceId(reference).orElseThrow();
        var paper=id==null?null:papers.findById(id).orElseThrow();
        return new Result(receipt.getId(),receipt.getProcessingStatus().name(),id,
                paper==null?null:paper.getExecutionPaperUniqueCode(),paper==null?null:paper.getIssuanceDate());
    }
    private String resolveOfficeCode(String officeName, String suppliedCode) {
        String supplied=suppliedCode==null ? null : SanimXmlValues.digits(suppliedCode.trim());
        var codes=new java.util.LinkedHashSet<String>();
        if (officeName!=null) {
            var matcher=java.util.regex.Pattern.compile("[0-9]+").matcher(SanimXmlValues.digits(officeName));
            while (matcher.find()) codes.add(matcher.group());
        }
        if (supplied!=null && !supplied.isBlank()) {
            if (codes.size()==1 && !codes.iterator().next().equals(supplied))
                throw SanimXmlValues.invalid("کد اداره واردشده با کد موجود در نام اداره ناسازگار است.");
            return supplied;
        }
        if (codes.size()!=1)
            throw SanimXmlValues.invalid("کد اداره از نام اداره قابل تشخیص نیست؛ کد اداره را دستی وارد کنید.");
        return codes.iterator().next();
    }
    public record Result(Long requestId,String status,Long executionPaperId,String number,LocalDateTime issuanceDate) {}
}
