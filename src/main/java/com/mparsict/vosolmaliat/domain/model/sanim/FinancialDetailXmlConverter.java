package com.mparsict.vosolmaliat.domain.model.sanim;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/** Lossless conversion of ordered financial detail rows to the document's XML storage format. */
@Converter
public class FinancialDetailXmlConverter implements AttributeConverter<String, String> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static DocumentBuilderFactory factory() throws Exception {
        var factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        return factory;
    }
    public String convertToDatabaseColumn(String json) {
        if (json == null) return null;
        try {
            List<Map<String,String>> rows=JSON.readValue(json, new TypeReference<>() {});
            var document=factory().newDocumentBuilder().newDocument();
            var root=document.createElement("details"); document.appendChild(root);
            for (var row:rows) {
                var element=document.createElement("item"); root.appendChild(element);
                for (var entry:row.entrySet()) {
                    var value=document.createElement("value"); value.setAttribute("name",entry.getKey());
                    if (entry.getValue()==null) value.setAttribute("null","true"); else value.setTextContent(entry.getValue());
                    element.appendChild(value);
                }
            }
            var transformer=TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
            var result=new StringWriter(); transformer.transform(new DOMSource(document),new StreamResult(result));
            return result.toString();
        } catch(Exception ex) { throw new IllegalArgumentException("جزئیات مالی باید فهرستی از عناوین و مبالغ باشد.", ex); }
    }
    public String convertToEntityAttribute(String xml) {
        if (xml==null) return null;
        try {
            var document=factory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            List<Map<String,String>> rows=new ArrayList<>();
            var items=document.getDocumentElement().getChildNodes();
            for(int i=0;i<items.getLength();i++) {
                if (!(items.item(i) instanceof Element item)) continue;
                Map<String,String> row=new LinkedHashMap<>();
                var values=item.getChildNodes();
                for(int j=0;j<values.getLength();j++) if(values.item(j) instanceof Element value)
                    row.put(value.getAttribute("name"),"true".equals(value.getAttribute("null"))?null:value.getTextContent());
                rows.add(row);
            }
            return JSON.writeValueAsString(rows);
        } catch(Exception ex) { throw new IllegalArgumentException("جزئیات مالی ذخیره‌شده نامعتبر است.", ex); }
    }
}
