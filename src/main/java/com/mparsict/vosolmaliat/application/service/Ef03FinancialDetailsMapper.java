package com.mparsict.vosolmaliat.application.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails;
import org.w3c.dom.*;
import java.util.*;

final class Ef03FinancialDetailsMapper {
    private static final ObjectMapper JSON = new ObjectMapper();
    static DebtFinancialDetails map(Document document, SanimXmlValues x) {
        List<Map<String, String>> penalties = new ArrayList<>();
        collect(document.getDocumentElement(), penalties);
        return new DebtFinancialDetails(single(x,"CB"), "[]", json(penalties), single(x,"EB"),
                single(x,"DB"), json(List.of(row(x,"FB"), row(x,"HB"))));
    }
    private static Map<String,String> row(SanimXmlValues x, String tag) {
        Map<String,String> result = new LinkedHashMap<>();
        result.put("tag",tag); result.put("amount",x.amount(tag).toPlainString());
        result.put("reported",String.valueOf(x.text(tag) != null));
        return result;
    }
    private static String single(SanimXmlValues x, String tag) { return json(List.of(row(x,tag))); }
    private static void collect(Element parent, List<Map<String,String>> result) {
        String description = null;
        for (Node node=parent.getFirstChild(); node!=null; node=node.getNextSibling()) {
            if (!(node instanceof Element child)) continue;
            String name = child.getLocalName() == null ? child.getNodeName() : child.getLocalName();
            if (name.equals("KB")) {
                if (description != null) result.add(missingPenalty(description));
                description = child.getTextContent().trim();
            }
            else if (name.equals("MB")) {
                Map<String,String> row = new LinkedHashMap<>();
                row.put("description",description == null ? "" : description);
                row.put("tag","MB"); row.put("amount",SanimXmlValues.decimal(child.getTextContent(),"MB").toPlainString());
                result.add(row); description=null;
            } else collect(child,result);
        }
        if (description != null) result.add(missingPenalty(description));
    }
    private static Map<String,String> missingPenalty(String description) {
        return Map.of("description",description,"tag","MB","amount","0","reported","false");
    }
    private static String json(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Cannot serialize financial details",e); }
    }
}
