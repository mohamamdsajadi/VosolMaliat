package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.model.integration.DebtPaperPushRecord;
import com.mparsict.vosolmaliat.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class Ef03ReceptionServiceTest {
    private final DebtPaperPushService push=mock(DebtPaperPushService.class);
    private final DebtPaperPushRecordRepository receipts=mock(DebtPaperPushRecordRepository.class);
    private final Ef03ReceptionService service=new Ef03ReceptionService(new DebtPaperXmlReader(),push,receipts,mock(ExecutionPaperRepository.class));
    private String xml(String office) {
        return "<content><MAIN><C>1403</C><R>1234567890</R><I>"+office+"</I></MAIN></content>";
    }
    @Test void extractsOfficeCodeAndKeepsFollowCodeInEnvelope() {
        when(push.push(any(),anyString())).thenReturn(null);
        when(receipts.findByReferenceId("reference")).thenReturn(Optional.of(DebtPaperPushRecord.builder()
                .id(1L).processingStatus(DebtPaperProcessingStatus.WAITING_IDENTITY).build()));
        var xml=xml("اداره امور مالیاتی ۲۰۲۸ اصفهان");
        service.receive(xml,"reference","registration-follow",null,"INCOME_TAX");
        var request=ArgumentCaptor.forClass(DebtPaperPushRequest.class);
        verify(push).push(request.capture(),anyString());
        assertThat(request.getValue().taxOfficeId()).isEqualTo("2028");
        assertThat(request.getValue().followCode()).isEqualTo("registration-follow");
        assertThat(request.getValue().xml()).isEqualTo(xml);
    }
    @Test void requiresManualCodeForMissingOrAmbiguousCode() {
        for (String office: new String[]{"اداره اصفهان","اداره 2028 منطقه 2"})
            assertThatThrownBy(()->service.receive(xml(office),"r","f",null,"INCOME_TAX"))
                    .hasMessageContaining("دستی");
        verifyNoInteractions(push);
    }
    @Test void rejectsConflictingManualCode() {
        assertThatThrownBy(()->service.receive(xml("اداره 2028"),"r","f","2029","INCOME_TAX"))
                .hasMessageContaining("ناسازگار");
        verifyNoInteractions(push);
    }
}
